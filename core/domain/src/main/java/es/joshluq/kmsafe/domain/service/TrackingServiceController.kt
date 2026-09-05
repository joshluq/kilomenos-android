package es.joshluq.kmsafe.domain.service

/**
 * Domain service interface to control the lifecycle of the location tracking service.
 */
interface TrackingServiceController {
    /**
     * Starts the background/foreground tracking service.
     */
    fun startTrackingService()

    /**
     * Stops the background/foreground tracking service.
     */
    fun stopTrackingService()
}
