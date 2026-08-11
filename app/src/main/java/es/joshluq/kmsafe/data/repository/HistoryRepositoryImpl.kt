package es.joshluq.kmsafe.data.repository

import es.joshluq.authkit.session.model.SessionState
import es.joshluq.foundationkit.coroutines.DispatcherProvider
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.data.local.dao.OdometerRecordDao
import es.joshluq.kmsafe.data.local.dao.TripRouteDao
import es.joshluq.kmsafe.data.local.entity.toDomain
import es.joshluq.kmsafe.data.local.entity.toEntity
import es.joshluq.kmsafe.data.mapper.*
import es.joshluq.kmsafe.data.remote.api.RentingApiService
import es.joshluq.kmsafe.data.remote.auth.UserSessionDataSource
import es.joshluq.kmsafe.data.remote.request.AddOdometerRecordRequest
import es.joshluq.kmsafe.data.remote.request.UpdateOdometerRecordRequest
import es.joshluq.kmsafe.data.remote.request.UploadRouteRequest
import es.joshluq.kmsafe.data.worker.SyncManager
import es.joshluq.kmsafe.domain.model.*
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

        // Remote Sync (Only if session is active and user has Cloud Sync feature)
        if (sessionDataSource.getSessionState().first() is SessionState.Active) {
            runCatching {
                if (sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
                    val response = apiService.addOdometerRecord(
                        contractId = record.contractId,
                        request = AddOdometerRecordRequest(
                            timestamp = record.timestamp.toIsoString(),
                            odometerValue = record.odometerValue,
                            label = record.label,
                            fuelConsumed = record.fuelAmount
                        )
                    )
                    if (response.isSuccessful) {
                        val remoteRecordDto = response.body()?.record
                        if (remoteRecordDto != null) {
                            val remoteRecord = remoteRecordDto.toDomain()
                            
                            // Swap ID logic for records and associated routes
                            if (remoteRecord.id != record.id) {
                                logger.d("HistoryRepository", "Swapping record ID from ${record.id} to ${remoteRecord.id}")
                                
                                // 1. Check if there was an associated route
                                routeDao.getRouteByRecordIdSync(record.id)?.let { localRouteEntity ->
                                    val localRoute = localRouteEntity.toDomain()
                                    // 2. Delete old and insert with new ID
                                    routeDao.deleteRouteByRecordId(record.id)
                                    routeDao.insertRoute(localRoute.copy(recordId = remoteRecord.id).toEntity())
                                    
                                    // 3. Sync the route with the new ID
                                    saveRoute(localRoute.copy(recordId = remoteRecord.id))
                                }
                                
                                dao.deleteRecord(recordToSave.toEntity())
                            }
                            
                            dao.insertRecord(remoteRecord.copy(syncStatus = SyncStatus.SYNCED).toEntity())
                            
                            // If IDs didn't change but we have a route, ensure it's synced
                            if (remoteRecord.id == record.id && record.hasRoute) {
                                routeDao.getRouteByRecordIdSync(record.id)?.let { 
                                    saveRoute(it.toDomain()) 
                                }
                            }
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

        // Remote Sync (Cloud Sync feature)
        if (sessionDataSource.getSessionState().first() is SessionState.Active && 
            sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
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

        // Remote Sync (Cloud Sync feature)
        if (sessionDataSource.getSessionState().first() is SessionState.Active && 
            sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
            logger.d("HistoryRepository", "Session active and Cloud Sync enabled, attempting remote deletion sync")
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

        if (!sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
            logger.d("HistoryRepository", "Sync skipped: Cloud Sync feature not enabled")
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

    override suspend fun saveRoute(route: TripRoute) = withContext(dispatchers.io) {
        logger.d("HistoryRepository", "Saving trip route locally for record: ${route.recordId}")
        routeDao.insertRoute(route.toEntity())
        
        // Remote Sync
        if (sessionDataSource.getSessionState().first() is SessionState.Active && 
            sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
            runCatching {
                val request = UploadRouteRequest(
                    encodedPolyline = route.encodedPolyline,
                    pointCount = route.pointCount
                )
                val response = apiService.uploadRoute(route.recordId, request)
                if (!response.isSuccessful) {
                    syncManager.scheduleSync()
                }
            }.onFailure {
                syncManager.scheduleSync()
            }
        }
    }

    override fun getRoute(recordId: String): Flow<TripRoute?> {
        return routeDao.getRouteByRecordId(recordId).map { it?.toDomain() }
    }

    override suspend fun deleteRoute(recordId: String) = withContext(dispatchers.io) {
        logger.d("HistoryRepository", "Deleting trip route for record: $recordId")
        routeDao.deleteRouteByRecordId(recordId)
        
        if (sessionDataSource.getSessionState().first() is SessionState.Active && 
            sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
            runCatching {
                val response = apiService.deleteRoute(recordId)
                if (!response.isSuccessful) {
                    syncManager.scheduleSync()
                }
            }.onFailure {
                syncManager.scheduleSync()
            }
        }
    }
}
