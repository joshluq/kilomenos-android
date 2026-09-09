package es.joshluq.kmsafe.infrastructure.repository

import es.joshluq.foundationkit.coroutines.DispatcherProvider
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.AuthSessionState
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.PricePoint
import es.joshluq.kmsafe.domain.model.PriceTrend
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.domain.model.StationPriceVolatility
import es.joshluq.kmsafe.domain.model.SyncStatus
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import es.joshluq.kmsafe.infrastructure.local.dao.FuelExpenseDao
import es.joshluq.kmsafe.infrastructure.local.dao.ServiceStationDao
import es.joshluq.kmsafe.infrastructure.local.entity.toDomain
import es.joshluq.kmsafe.infrastructure.local.entity.toEntity
import es.joshluq.kmsafe.infrastructure.mapper.toDomain as toDomainFromApi
import es.joshluq.kmsafe.infrastructure.mapper.toRequest
import es.joshluq.kmsafe.infrastructure.remote.api.FuelApiService
import es.joshluq.kmsafe.infrastructure.remote.auth.UserSessionDataSource
import es.joshluq.kmsafe.infrastructure.repository.util.SyncIdHandler
import es.joshluq.kmsafe.infrastructure.worker.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [ServiceStationRepository] using Room database.
 *
 * @property stationDao Room DAO for service stations.
 * @property expenseDao Room DAO for fuel expenses to derive volatility analytics.
 * @property logger Logger utility.
 * @property dispatchers Coroutine dispatcher provider.
 */
@Singleton
class ServiceStationRepositoryImpl @Inject constructor(
    private val stationDao: ServiceStationDao,
    private val expenseDao: FuelExpenseDao,
    private val apiService: FuelApiService,
    private val sessionDataSource: UserSessionDataSource,
    private val syncIdHandler: SyncIdHandler,
    private val syncManager: SyncManager,
    private val logger: LoggerKit,
    private val dispatchers: DispatcherProvider
) : ServiceStationRepository {

    override fun getAllStations(): Flow<List<ServiceStation>> {
        return stationDao.getAllStations().map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(dispatchers.io)
    }

    override fun getFavoriteStations(): Flow<List<ServiceStation>> {
        return stationDao.getFavoriteStations().map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(dispatchers.io)
    }

    override fun getStationById(id: String): Flow<ServiceStation?> {
        return stationDao.getStationById(id).map { it?.toDomain() }
            .flowOn(dispatchers.io)
    }

    override fun saveStation(station: ServiceStation): Flow<String> = flow {
        logger.d("ServiceStationRepository", "Saving station: ${station.name} (${station.id})")
        
        // 1. Local-First save
        val stationToSave = station.copy(syncStatus = SyncStatus.PENDING)
        stationDao.insertStation(stationToSave.toEntity())
        
        var finalId = station.id

        // 2. Remote Sync
        if (sessionDataSource.getSessionState().first() is AuthSessionState.Active) {
            runCatching {
                if (sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
                    val response = apiService.syncStations(listOf(station.toRequest()))
                    if (response.isSuccessful) {
                        logger.i("ServiceStationRepository", "Remote station sync successful")
                        val remoteStation = response.body()?.stations?.firstOrNull()?.toDomainFromApi()
                        if (remoteStation != null) {
                            syncIdHandler.resolveStationId(station, remoteStation)
                            finalId = remoteStation.id
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: ""
                        logger.e("ServiceStationRepository", "Remote station sync failed with code ${response.code()}: $errorBody")
                        syncManager.scheduleSync()
                    }
                }
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                logger.e("ServiceStationRepository", "Remote station sync failed", e)
                syncManager.scheduleSync()
            }
        }
        
        emit(finalId)
    }.flowOn(dispatchers.io)

    override fun setFavorite(stationId: String, isFavorite: Boolean): Flow<Unit> = flow {
        logger.d("ServiceStationRepository", "Setting favorite=$isFavorite for station $stationId")
        stationDao.updateFavorite(stationId, isFavorite)
        emit(Unit)
    }.flowOn(dispatchers.io)

    override fun deleteStation(id: String): Flow<Unit> = flow {
        logger.d("ServiceStationRepository", "Deleting station ID: $id")
        
        // 1. Local delete
        stationDao.deleteStation(id)

        // 2. Remote delete
        if (sessionDataSource.getSessionState().first() is AuthSessionState.Active) {
            if (sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
                runCatching {
                    val response = apiService.deleteStation(id)
                    if (!response.isSuccessful) {
                        val errorBody = response.errorBody()?.string() ?: ""
                        logger.e("ServiceStationRepository", "Remote station delete failed with code ${response.code()}: $errorBody")
                        syncManager.scheduleSync()
                    }
                }.onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    logger.e("ServiceStationRepository", "Remote station delete failed", e)
                    syncManager.scheduleSync()
                }
            }
        }
        
        emit(Unit)
    }.flowOn(dispatchers.io)

    override fun clearAllStations(): Flow<Unit> = flow {
        logger.i("ServiceStationRepository", "Clearing all local stations")
        stationDao.clearAllStations()
        emit(Unit)
    }.flowOn(dispatchers.io)

    override fun syncStations(): Flow<List<ServiceStation>> = flow {
        logger.d("ServiceStationRepository", "Starting station synchronization")

        if (!sessionDataSource.hasFeature(Feature.CLOUD_SYNC.id)) {
            logger.d("ServiceStationRepository", "Sync skipped: Cloud Sync feature not enabled")
            emit(emptyList())
            return@flow
        }

        val response = apiService.getStations()
        if (response.isSuccessful) {
            val stations = response.body()?.stations?.map { it.toDomainFromApi() } ?: emptyList()
            logger.i("ServiceStationRepository", "Sync successful: Found ${stations.size} remote stations")

            stations.forEach { station ->
                stationDao.insertStation(station.copy(syncStatus = SyncStatus.SYNCED).toEntity())
            }
            emit(stations)
        } else {
            logger.e("ServiceStationRepository", "Sync failed with HTTP error: ${response.code()}")
            throw KmException(KmError.NetworkError)
        }
    }.flowOn(dispatchers.io)

    override fun syncStationBatch(stations: List<ServiceStation>): Flow<List<ServiceStation>> = flow {
        logger.d("ServiceStationRepository", "Syncing batch of ${stations.size} stations")

        if (stations.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        val response = apiService.syncStations(stations.map { it.toRequest() })
        if (response.isSuccessful) {
            val remoteStations = response.body()?.stations?.map { it.toDomainFromApi() } ?: emptyList()
            remoteStations.forEach { remote ->
                val local = stations.find { it.id == remote.id }
                if (local != null) {
                    syncIdHandler.resolveStationId(local, remote)
                }
            }
            emit(remoteStations)
        } else {
            logger.e("ServiceStationRepository", "Batch sync failed")
            throw KmException(KmError.NetworkError)
        }
    }.flowOn(dispatchers.io)

    override fun getStationVolatility(stationId: String, fuelType: FuelType): Flow<StationPriceVolatility?> {
        return expenseDao.getExpensesByStationAndFuel(stationId, fuelType.name).map { expenses ->
            if (expenses.isEmpty()) {
                null
            } else {
                val sorted = expenses.sortedBy { it.timestamp }
                val currentPrice = sorted.last().unitPrice
                val prices = sorted.map { it.unitPrice }
                val avgPrice = prices.average()
                val minPrice = prices.minOrNull() ?: currentPrice
                val maxPrice = prices.maxOrNull() ?: currentPrice

                val priceTrend = when {
                    currentPrice < avgPrice - 0.02 -> PriceTrend.CHEAPER
                    currentPrice > avgPrice + 0.02 -> PriceTrend.EXPENSIVE
                    else -> PriceTrend.AVERAGE
                }

                val priceHistory = sorted.map { PricePoint(it.timestamp, it.unitPrice) }

                StationPriceVolatility(
                    stationId = stationId,
                    fuelType = fuelType,
                    currentPrice = currentPrice,
                    historicalAveragePrice = avgPrice,
                    minRecordedPrice = minPrice,
                    maxRecordedPrice = maxPrice,
                    priceTrend = priceTrend,
                    priceHistory = priceHistory
                )
            }
        }.flowOn(dispatchers.io)
    }

    override fun getStationsInBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): Flow<List<ServiceStation>> {
        return stationDao.getStationsInBoundingBox(minLat, maxLat, minLng, maxLng)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }
}
