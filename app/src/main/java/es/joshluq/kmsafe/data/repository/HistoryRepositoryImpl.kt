package es.joshluq.kmsafe.data.repository

import es.joshluq.authkit.session.model.SessionState
import es.joshluq.foundationkit.coroutines.DispatcherProvider
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.data.local.dao.OdometerRecordDao
import es.joshluq.kmsafe.data.local.entity.toDomain
import es.joshluq.kmsafe.data.local.entity.toEntity
import es.joshluq.kmsafe.data.mapper.ErrorMapper
import es.joshluq.kmsafe.data.mapper.toDomain
import es.joshluq.kmsafe.data.mapper.toIsoString
import es.joshluq.kmsafe.data.remote.api.RentingApiService
import es.joshluq.kmsafe.data.remote.auth.UserSessionDataSource
import es.joshluq.kmsafe.data.remote.request.AddOdometerRecordRequest
import es.joshluq.kmsafe.data.remote.request.UpdateOdometerRecordRequest
import es.joshluq.kmsafe.data.worker.SyncManager
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.SyncStatus
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of [HistoryRepository] using Room and Remote API.
 */
class HistoryRepositoryImpl @Inject constructor(
    private val dao: OdometerRecordDao,
    private val apiService: RentingApiService,
    private val sessionDataSource: UserSessionDataSource,
    private val syncManager: SyncManager,
    private val errorMapper: ErrorMapper,
    private val logger: LoggerKit,
    private val dispatchers: DispatcherProvider
) : HistoryRepository {

    override fun getHistory(contractId: String): Flow<List<OdometerRecord>> {
        return dao.getAllRecords(contractId).map { entities ->
            entities.map { it.toDomain() }
        }.onEach {
            logger.d("HistoryRepository", "History for contract $contractId fetched: ${it.size} records")
        }
    }

    override suspend fun getPendingRecords(): List<OdometerRecord> {
        return dao.getAllPendingRecords().map { it.toDomain() }
    }

    override suspend fun saveRecord(record: OdometerRecord) = withContext(dispatchers.io) {
        logger.d("HistoryRepository", "Saving record: ${record.odometerValue} km")
        
        // Local-First: Always save as PENDING to allow future migration
        val recordToSave = record.copy(syncStatus = SyncStatus.PENDING)
        dao.insertRecord(recordToSave.toEntity())

        // Remote Sync (Only if session is active and user is Premium)
        if (sessionDataSource.getSessionState().first() is SessionState.Active) {
            runCatching {
                if (sessionDataSource.isPremium()) {
                    val response = apiService.addOdometerRecord(
                        contractId = record.contractId,
                        request = AddOdometerRecordRequest(
                            timestamp = record.timestamp.toIsoString(),
                            odometerValue = record.odometerValue,
                            label = record.label,
                            fuelAmount = record.fuelAmount
                        )
                    )
                    if (response.isSuccessful) {
                        val remoteRecordDto = response.body()?.record
                        if (remoteRecordDto != null) {
                            val remoteRecord = remoteRecordDto.toDomain()
                            if (remoteRecord.id != record.id) {
                                dao.deleteRecord(recordToSave.toEntity())
                            }
                            dao.insertRecord(remoteRecord.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                        }
                    } else {
                        syncManager.scheduleSync()
                    }
                }
            }.onFailure {
                syncManager.scheduleSync()
            }
        }
    }

    override suspend fun updateRecord(record: OdometerRecord) = withContext(dispatchers.io) {
        logger.d("HistoryRepository", "Updating record ID: ${record.id}")
        // Local-First: Update in DB with PENDING status
        val recordToUpdate = record.copy(syncStatus = SyncStatus.PENDING)
        dao.insertRecord(recordToUpdate.toEntity())

        // Remote Sync (Premium feature)
        val isPremium = sessionDataSource.isPremium()

        if (sessionDataSource.getSessionState().first() is SessionState.Active && isPremium) {
            logger.d("HistoryRepository", "Session active and Premium, attempting remote update sync")
            runCatching {
                val response = apiService.updateOdometerRecord(
                    recordId = record.id,
                    request = UpdateOdometerRecordRequest(
                        odometerValue = record.odometerValue,
                        timestamp = record.timestamp.toIsoString(),
                        label = record.label,
                        fuelAmount = record.fuelAmount
                    )
                )
                if (response.isSuccessful) {
                    logger.i("HistoryRepository", "Remote update successful")
                    // Success: Mark as SYNCED locally
                    dao.insertRecord(record.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                } else {
                    logger.e("HistoryRepository", "Remote update failed with code: ${response.code()}")
                    syncManager.scheduleSync()
                }
            }.onFailure {
                logger.e("HistoryRepository", "Error during remote update sync", it)
                syncManager.scheduleSync()
            }
        }
    }

    override suspend fun deleteRecord(record: OdometerRecord) = withContext(dispatchers.io) {
        logger.d("HistoryRepository", "Deleting record ID: ${record.id}")
        // Local-First: Delete from DB
        dao.deleteRecord(record.toEntity())

        // Remote Sync (Premium feature)
        val isPremium = sessionDataSource.isPremium()

        if (sessionDataSource.getSessionState().first() is SessionState.Active && isPremium) {
            logger.d("HistoryRepository", "Session active and Premium, attempting remote deletion sync")
            runCatching {
                val response = apiService.deleteOdometerRecord(record.id)
                if (response.isSuccessful) {
                    logger.i("HistoryRepository", "Remote deletion successful")
                } else {
                    logger.e("HistoryRepository", "Remote deletion failed with code: ${response.code()}")
                }
            }.onFailure {
                logger.e("HistoryRepository", "Error during remote deletion sync", it)
            }
        }
    }

    override fun syncHistory(contractId: String): Flow<Unit> = flow {
        logger.d("HistoryRepository", "Starting history synchronization for contract: $contractId")

        if (!sessionDataSource.isPremium()) {
            logger.d("HistoryRepository", "Sync skipped: User is not Premium")
            return@flow
        }

        val response = apiService.getOdometerRecords(contractId)
        if (response.isSuccessful) {
            val records = response.body()?.records?.map { it.toDomain() } ?: emptyList()
            logger.i("HistoryRepository", "Sync successful: Found ${records.size} remote records")

            records.forEach { record ->
                dao.insertRecord(record.toEntity())
            }
            emit(Unit)
        } else {
            logger.e("HistoryRepository", "Sync failed with HTTP error: ${response.code()}")
            throw KmException(errorMapper.mapApiResponse(response))
        }
    }.flowOn(dispatchers.io)
}
