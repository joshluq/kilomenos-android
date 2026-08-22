package es.joshluq.kmsafe.core.domain.service

import es.joshluq.kmsafe.domain.model.ServiceStation

/**
 * Service for showing proactive notifications related to service stations.
 */
interface StationNotificationService {

    /**
     * Shows a notification prompting the user to record a price or refuel
     * when they are detected at a specific station.
     */
    fun showStationProximityPrompt(station: ServiceStation)

    /**
     * Cancels any active station proximity notification.
     */
    fun cancelProximityPrompt()
}
