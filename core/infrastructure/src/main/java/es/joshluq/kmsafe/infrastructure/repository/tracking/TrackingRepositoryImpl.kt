package es.joshluq.kmsafe.infrastructure.repository.tracking

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.infrastructure.local.datasource.TrackingDataSource
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [TrackingRepository] for orchestrating manual and automated trip tracking.
 *
 * Delegates state persistence to [TrackingDataSource] to ensure repository statelessness
 * and delegates activity transition updates to [AutoTrackingManager].
 *
 * @property dataSource Persistent and reactive storage for current tracking metrics.
 * @property autoTrackingManager Manager for registering and responding to activity transitions.
 * @property logger Logger utility for debugging trip tracking flows.
 */
@Singleton
class TrackingRepositoryImpl @Inject constructor(
    private val dataSource: TrackingDataSource,
    private val autoTrackingManager: AutoTrackingManager,
    private val logger: LoggerKit
) : TrackingRepository {

    /**
     * Flow emitting the accumulated distance in meters for the active trip.
     */
    override val currentDistanceMeters: Flow<Double> = dataSource.getDistanceMeters()

    /**
     * Flow emitting the start timestamp (in milliseconds) of the current tracking session, or null if inactive.
     */
    override val startTime: Flow<Long?> = dataSource.getStartTime()

    /**
     * Flow emitting true when a tracking session is actively recording GPS points.
     */
    override val isTracking: Flow<Boolean> = dataSource.isTracking()

    /**
     * Flow emitting the encoded Google Polyline string representing the path traversed so far.
     */
    override val currentRoutePolyline: Flow<String?> = dataSource.getRoutePolyline()

    /**
     * Flow emitting the total number of GPS coordinates recorded in the current polyline.
     */
    override val pointCount: Flow<Int> = dataSource.getPointCount()

    /**
     * Starts a new tracking session by saving the current start timestamp in the data source.
     */
    override suspend fun startTracking() {
        logger.i("TrackingRepository", "startTracking initiated")
        dataSource.startTracking(System.currentTimeMillis())
    }

    /**
     * Updates tracking state with new distance increments and optional coordinate points.
     *
     * @param distanceMeters Additional distance increment in meters.
     * @param latitude Optional latitude to append to the encoded route polyline.
     * @param longitude Optional longitude to append to the encoded route polyline.
     */
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

    /**
     * Stops the active tracking session while retaining recorded distance and route metrics.
     */
    override suspend fun stopTracking() {
        logger.i("TrackingRepository", "stopTracking initiated")
        dataSource.stopTracking()
        logger.d("TrackingRepository", "stopTracking completed in data source")
    }

    /**
     * Clears all recorded tracking distance, points, and polyline from persistent storage.
     */
    override suspend fun clear() {
        logger.d("TrackingRepository", "clear tracking data initiated")
        dataSource.clear()
    }

    /**
     * Requests automatic trip tracking registration based on physical activity recognition.
     */
    override fun startAutoTracking() {
        logger.i("TrackingRepository", "startAutoTracking initiated")
        autoTrackingManager.startAutoTracking()
    }

    /**
     * Unregisters automatic trip tracking activity transition updates.
     */
    override fun stopAutoTracking() {
        logger.i("TrackingRepository", "stopAutoTracking initiated")
        autoTrackingManager.stopAutoTracking()
    }
}
