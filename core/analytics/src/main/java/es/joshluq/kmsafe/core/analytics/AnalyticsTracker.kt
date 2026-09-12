package es.joshluq.kmsafe.core.analytics

import es.joshluq.kmsafe.core.analytics.model.KmsafeAnalyticsEvent

/**
 * High-level domain contract for tracking analytics and telemetry.
 * Decouples presentation and domain modules from underlying SDKs or kits.
 */
interface AnalyticsTracker {
    /**
     * Tracks a strongly-typed domain analytics event.
     */
    fun track(event: KmsafeAnalyticsEvent)

    /**
     * Tracks a screen view event.
     */
    fun trackScreen(screenName: String, screenClass: String? = null)

    /**
     * Sets or removes a global user property.
     */
    fun setUserProperty(key: String, value: String?)
}
