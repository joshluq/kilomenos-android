package es.joshluq.kmsafe.data.location

import es.joshluq.kmsafe.data.local.datasource.TrackingDataSource
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingRepositoryImpl @Inject constructor(
    private val dataSource: TrackingDataSource,
    private val autoTrackingManager: AutoTrackingManager
) : TrackingRepository {

    override val currentDistanceMeters: Flow<Double> = dataSource.getDistanceMeters()

    override val startTime: Flow<Long?> = dataSource.getStartTime()

    override val isTracking: Flow<Boolean> = dataSource.isTracking()

    override suspend fun startTracking() {
        dataSource.startTracking(System.currentTimeMillis())
    }

    override suspend fun updateDistance(meters: Double) {
        dataSource.updateDistance(meters)
    }

    override suspend fun stopTracking() {
        dataSource.stopTracking()
    }

    override suspend fun clear() {
        dataSource.clear()
    }

    override fun startAutoTracking() {
        autoTrackingManager.startAutoTracking()
    }

    override fun stopAutoTracking() {
        autoTrackingManager.stopAutoTracking()
    }
}
