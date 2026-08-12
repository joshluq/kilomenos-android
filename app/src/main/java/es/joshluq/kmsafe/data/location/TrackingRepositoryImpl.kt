package es.joshluq.kmsafe.data.location

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.data.local.datasource.TrackingDataSource
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

    override val currentRoutePolyline: Flow<String?> = dataSource.getRoutePolyline()

    override val pointCount: Flow<Int> = dataSource.getPointCount()

    override suspend fun startTracking() {
        logger.i("TrackingRepository", "startTracking initiated")
        dataSource.startTracking(System.currentTimeMillis())
    }

    override suspend fun updateTracking(distanceMeters: Double, latitude: Double?, longitude: Double?) {
        if (latitude != null && longitude != null) {
            val currentPolyline = dataSource.getRoutePolyline().first() ?: ""
            val points = PolyUtil.decode(currentPolyline).toMutableList()

            // Add new point
            points.add(LatLng(latitude, longitude))

            val newPolyline = PolyUtil.encode(points)
            dataSource.updateTracking(distanceMeters, newPolyline, points.size)
        } else {
            // Distance-only update
            val currentPolyline = dataSource.getRoutePolyline().first() ?: ""
            val count = dataSource.getPointCount().first()
            dataSource.updateTracking(distanceMeters, currentPolyline, count)
        }
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
