package es.joshluq.kmsafe.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for managing the state of the current active trip tracking.
 */
interface TrackingRepository {
    /**
     * The total distance accumulated during the current trip in meters.
     */
    val currentDistanceMeters: StateFlow<Double>

    /**
     * The start timestamp of the current trip.
     */
    val startTime: StateFlow<Long?>

    /**
     * Whether tracking is currently active.
     */
    val isTracking: StateFlow<Boolean>

    /**
     * Starts a new trip tracking session.
     */
    fun startTracking()

    /**
     * Updates the accumulated distance.
     */
    fun updateDistance(meters: Double)

    /**
     * Stops and resets the current tracking session.
     */
    fun stopTracking()

    /**
     * Clears all tracking data and returns to initial state.
     */
    fun clear()

    /**
     * Registers for background activity transitions (Auto-Tracking).
     */
    fun startAutoTracking()

    /**
     * Unregisters from background activity transitions.
     */
    fun stopAutoTracking()
}
