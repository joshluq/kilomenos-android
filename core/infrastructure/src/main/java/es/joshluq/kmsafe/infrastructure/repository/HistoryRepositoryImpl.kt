package es.joshluq.kmsafe.infrastructure.repository

import androidx.room.withTransaction
import es.joshluq.kmsafe.domain.model.AuthSessionState
import es.joshluq.foundationkit.coroutines.DispatcherProvider
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.infrastructure.local.AppDatabase
import es.joshluq.kmsafe.infrastructure.local.dao.OdometerRecordDao
import es.joshluq.kmsafe.infrastructure.local.dao.TripRouteDao
import es.joshluq.kmsafe.infrastructure.local.entity.toDomain as toDomainFromEntity
import es.joshluq.kmsafe.infrastructure.local.entity.toEntity
import es.joshluq.kmsafe.infrastructure.mapper.ErrorMapper
import es.joshluq.kmsafe.infrastructure.mapper.toDomain as toDomainFromApi
import es.joshluq.kmsafe.infrastructure.mapper.toIsoString
import es.joshluq.kmsafe.infrastructure.remote.api.RentingApiService
import es.joshluq.kmsafe.infrastructure.remote.auth.UserSessionDataSource
import es.joshluq.kmsafe.infrastructure.remote.request.AddOdometerRecordRequest
import es.joshluq.kmsafe.infrastructure.remote.request.UpdateOdometerRecordRequest
import es.joshluq.kmsafe.infrastructure.remote.request.UploadRouteRequest
import es.joshluq.kmsafe.infrastructure.repository.util.SyncIdHandler
import es.joshluq.kmsafe.infrastructure.worker.SyncManager
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.SyncStatus
import es.joshluq.kmsafe.domain.model.TripRoute
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
    private val routeDao: TripRouteDao,
    private val appDatabase: AppDatabase,
    private val apiService: RentingApiService,
    private val sessionDataSource: UserSessionDataSource,
    private val syncIdHandler: SyncIdHandler,
    private val syncManager: SyncManager,
    private val errorMapper: ErrorMapper,
    private val logger: LoggerKit,
    private val dispatchers: DispatcherProvider
) : HistoryRepository {

    override fun getHistory(contractId: String): Flow<List<OdometerRecord>> {
        return dao.getAllRecords(contractId).map { entities ->
            entities.map { it.toDomainFromEntity() }
        }.onEach {
            logger.d("HistoryRepository", "History for contract $contractId fetched: ${it.size} records")
        }
    }

    override suspend fun getPendingRecords(): List<OdometerRecord> {
        return dao.getAllPendingRecords().map { it.toDomainFromEntity() }
    }

    override suspend fun saveRecord(record: OdometerRecord, route: TripRoute?) = withContext(dispatchers.io) {
        logger.d("HistoryRepository", "Saving record: ${record.odometerValue} km")

        // Local-First: Always save as PENDING to allow future migration
        val recordToSave = record.copy(syncStatus = SyncStatus.PENDING)
        dao.insertRecord(recordToSave.toEntity())

        // Save Route if present
        route?.let {
            logger.d("HistoryRepository", "Saving associated trip route for record ${record.id}")
            routeDao.insertRoute(it.copy(recordId = record.id).toEntity())
        }

        // Remote Sync (Only if session is active and user has Cloud Sync feature)
        if (sessionDataSource.getSessionState().first() is AuthSessionState.Active) {
            // Initial records are created automatically by the server during contract creation.
            // We skip explicit POST here to avoid duplication.
            if (record.isInitialRecord) {
                logger.d("HistoryRepository", "Skipping remote sync for initial record ${record.id}")
                return@withContext
            }

            runCatching {
                if (sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
                    val response = apiService.addOdometerRecord(
                        contractId = record.contractId,
                        request = AddOdometerRecordRequest(
                            id = record.id,
                            timestamp = record.timestamp.toIsoString(),
                            odometerValue = record.odometerValue,
                            label = record.label,
                            fuelConsumed = record.fuelAmount
                        )
                    )
                    if (response.isSuccessful) {
                        val remoteRecordDto = response.body()?.record
                        if (remoteRecordDto != null) {
                            val remoteRecord = remoteRecordDto.toDomainFromApi()
                            syncIdHandler.resolveOdometerId(record, remoteRecord)

                            if (remoteRecord.id == record.id && (route != null || record.hasRoute)) {
                                val routeToSync = route ?: routeDao.getRouteByRecordIdSync(record.id)?.toDomainFromEntity()
                                routeToSync?.let { saveRoute(it) }
                            }
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: ""
                        logger.e("HistoryRepository", "Remote record creation failed with code ${response.code()}: $errorBody")
                        if (response.code() == 409 || errorBody.contains("duplicate key", ignoreCase = true) || errorBody.contains("already exists", ignoreCase = true)) {
                            logger.w("HistoryRepository", "Remote sync conflict: Duplicate key or already exists. Marking as SYNCED.")
                            dao.insertRecord(record.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                        } else {
                            syncManager.scheduleSync()
                        }
                    }
                }
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                logger.e("HistoryRepository", "Remote record creation failed", e)
                syncManager.scheduleSync()
            }
        }
    }

    override suspend fun updateRecord(record: OdometerRecord) = withContext(dispatchers.io) {
        logger.d("HistoryRepository", "Updating record ID: ${record.id}")
        // Local-First: Update in DB with PENDING status
        val recordToUpdate = record.copy(syncStatus = SyncStatus.PENDING)
        dao.insertRecord(recordToUpdate.toEntity())

        // Remote Sync (Cloud Sync feature)
        if (sessionDataSource.getSessionState().first() is AuthSessionState.Active &&
            sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)
        ) {
            logger.d("HistoryRepository", "Session active and Cloud Sync enabled, attempting remote update sync")
            runCatching {
                val response = apiService.updateOdometerRecord(
                    recordId = record.id,
                    request = UpdateOdometerRecordRequest(
                        odometerValue = record.odometerValue,
                        timestamp = record.timestamp.toIsoString(),
                        label = record.label,
                        fuelConsumed = record.fuelAmount
                    )
                )
                if (response.isSuccessful) {
                    logger.i("HistoryRepository", "Remote update successful")
                    // Success: Mark as SYNCED locally
                    dao.insertRecord(record.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    logger.e("HistoryRepository", "Remote update failed with code ${response.code()}: $errorBody")
                    syncManager.scheduleSync()
                }
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                logger.e("HistoryRepository", "Error during remote update sync", e)
                syncManager.scheduleSync()
            }
        }
    }

    override suspend fun deleteRecord(record: OdometerRecord) = withContext(dispatchers.io) {
        logger.d("HistoryRepository", "Deleting record ID: ${record.id}")
        // Local-First: Delete from DB
        dao.deleteRecord(record.toEntity())

        // Remote Sync (Cloud Sync feature)
        if (sessionDataSource.getSessionState().first() is AuthSessionState.Active &&
            sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)
        ) {
            logger.d("HistoryRepository", "Session active and Cloud Sync enabled, attempting remote deletion sync")
            runCatching {
                val response = apiService.deleteOdometerRecord(record.id)
                if (response.isSuccessful) {
                    logger.i("HistoryRepository", "Remote deletion successful")
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    logger.e("HistoryRepository", "Remote deletion failed with code ${response.code()}: $errorBody")
                }
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                logger.e("HistoryRepository", "Error during remote deletion sync", e)
            }
        }
    }

    override fun syncHistory(contractId: String): Flow<Unit> = flow {
        logger.d("HistoryRepository", "Starting history synchronization for contract: $contractId")

        if (!sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
            logger.d("HistoryRepository", "Sync skipped: Cloud Sync feature not enabled")
            return@flow
        }

        val response = apiService.getOdometerRecords(contractId)
        if (response.isSuccessful) {
            val records = response.body()?.records?.map { it.toDomainFromApi() } ?: emptyList()
            logger.i("HistoryRepository", "Sync successful: Found ${records.size} remote records")

            if (records.isNotEmpty()) {
                appDatabase.withTransaction {
                    dao.insertRecords(records.map { it.toEntity() })
                }
            }
            emit(Unit)
        } else {
            logger.e("HistoryRepository", "Sync failed with HTTP error: ${response.code()}")
            throw KmException(errorMapper.mapApiResponse(response))
        }
    }.flowOn(dispatchers.io)

    override suspend fun saveRoute(route: TripRoute) = withContext(dispatchers.io) {
        logger.d("HistoryRepository", "Saving trip route locally for record: ${route.recordId}")
        
        appDatabase.withTransaction {
            routeDao.insertRoute(route.toEntity())
            dao.updateHasRoute(route.recordId, true)
        }

        // Remote Sync
        if (sessionDataSource.getSessionState().first() is AuthSessionState.Active &&
            sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)
        ) {
            runCatching {
                val request = UploadRouteRequest(
                    encodedPolyline = route.encodedPolyline,
                    pointCount = route.pointCount
                )
                val response = apiService.uploadRoute(route.recordId, request)
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string() ?: ""
                    logger.e("HistoryRepository", "Upload route failed with code ${response.code()}: $errorBody")
                    syncManager.scheduleSync()
                }
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                logger.e("HistoryRepository", "Error during route upload", e)
                syncManager.scheduleSync()
            }
        }
    }

    override fun getRoute(recordId: String): Flow<TripRoute?> = flow {
        // 1. Try to get it from Local DB first
        val localRoute = routeDao.getRouteByRecordIdSync(recordId)?.toDomainFromEntity()
        if (localRoute != null) {
            emit(localRoute)
            return@flow
        }

        // 2. If not local, check if the record exists and has a route flag
        val recordEntity = dao.getRecordByIdSync(recordId)
        val hasRouteFlag = recordEntity?.hasRoute == true

        if (hasRouteFlag && sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
            logger.d("HistoryRepository", "Route not found locally but flag is true. Fetching from remote...")

            runCatching {
                val response = apiService.getRoute(recordId)
                if (response.isSuccessful) {
                    val remoteRoute = response.body()?.route?.toDomainFromApi()
                    if (remoteRoute != null) {
                        // 3. Persist locally for future offline use
                        routeDao.insertRoute(remoteRoute.toEntity())
                        emit(remoteRoute)
                    }
                }
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                logger.e("HistoryRepository", "Failed to fetch remote route for record $recordId", e)
            }
        }
    }.flowOn(dispatchers.io)

    override suspend fun deleteRoute(recordId: String) = withContext(dispatchers.io) {
        logger.d("HistoryRepository", "Deleting trip route for record: $recordId")
        routeDao.deleteRouteByRecordId(recordId)

        if (sessionDataSource.getSessionState().first() is AuthSessionState.Active &&
            sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)
        ) {
            runCatching {
                val response = apiService.deleteRoute(recordId)
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string() ?: ""
                    logger.e("HistoryRepository", "Delete route failed with code ${response.code()}: $errorBody")
                    syncManager.scheduleSync()
                }
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                logger.e("HistoryRepository", "Error during route delete", e)
                syncManager.scheduleSync()
            }
        }
    }
}
