package es.joshluq.kmsafe.data.location

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.data.local.datasource.TrackingDataSource
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingRepositoryImpl @Inject constructor(
    private val dataSource: TrackingDataSource,
    private val autoTrackingManager: AutoTrackingManager,
    private val logger: LoggerKit
) : TrackingRepository {

    override val currentDistanceMeters: Flow<Double> = dataSource.getDistanceMeters()

    override val startTime: Flow<Long?> = dataSource.getStartTime()

    override val isTracking: Flow<Boolean> = dataSource.isTracking()

    override suspend fun startTracking() {
        logger.i("TrackingRepository", "startTracking initiated")
        dataSource.startTracking(System.currentTimeMillis())
    }

    override suspend fun updateDistance(meters: Double) {
        dataSource.updateDistance(meters)
    }

    override suspend fun stopTracking() {
        logger.i("TrackingRepository", "stopTracking initiated")
        dataSource.stopTracking()
        logger.d("TrackingRepository", "stopTracking completed in data source")
    }

    override suspend fun clear() {
        logger.d("TrackingRepository", "clear tracking data initiated")
        dataSource.clear()
    }

    override fun startAutoTracking() {
        logger.i("TrackingRepository", "startAutoTracking initiated")
        autoTrackingManager.startAutoTracking()
    }

    override fun stopAutoTracking() {
        logger.i("TrackingRepository", "stopAutoTracking initiated")
        autoTrackingManager.stopAutoTracking()
    }
}
