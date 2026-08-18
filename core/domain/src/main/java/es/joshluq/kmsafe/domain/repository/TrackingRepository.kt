package es.joshluq.kmsafe.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing the state of the current active trip tracking.
 */
interface TrackingRepository {
    /**
     * The total distance accumulated during the current trip in meters.
     */
    val currentDistanceMeters: Flow<Double>

    /**
     * The start timestamp of the current trip.
     */
    val startTime: Flow<Long?>

    /**
     * Whether tracking is currently active.
     */
    val isTracking: Flow<Boolean>

    /**
     * The current route encoded as a polyline string.
     */
    val currentRoutePolyline: Flow<String?>

    /**
     * The number of points in the current route.
     */
    val pointCount: Flow<Int>

    /**
     * Starts a new trip tracking session.
     */
    suspend fun startTracking()

    /**
     * Updates the accumulated distance and optionally appends a new point to the route.
     */
    suspend fun updateTracking(distanceMeters: Double, latitude: Double? = null, longitude: Double? = null)

    /**
     * Stops and resets the current tracking session.
     */
    suspend fun stopTracking()

    /**
     * Clears all tracking data and returns to initial state.
     */
    suspend fun clear()

    /**
     * Registers for background activity transitions (Auto-Tracking).
     */
    fun startAutoTracking()

    /**
     * Unregisters from background activity transitions.
     */
    fun stopAutoTracking()
}
