package es.joshluq.kmsafe.core.analytics.provider

import es.joshluq.analyticskit.data.provider.AnalyticsProvider
import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.foundationkit.log.LoggerKit
import javax.inject.Inject

/**
 * An [AnalyticsProvider] that routes events to [LoggerKit].
 * Useful for development and debugging.
 */
class LoggerAnalyticsProvider @Inject constructor(
    private val logger: LoggerKit
) : AnalyticsProvider {

    companion object {
        private const val TAG = "Analytics"
    }

    override val key: String = "LoggerProvider"

    override suspend fun track(event: AnalyticsEvent) {
        when (event) {
            is AnalyticsEvent.Custom -> {
                logger.d(TAG, "Event: ${event.name} | Properties: ${event.properties}")
            }
            is AnalyticsEvent.ScreenView -> {
                logger.d(TAG, "Screen View: ${event.screenName} (${event.screenClass ?: "N/A"})")
            }
            is AnalyticsEvent.FunnelStep -> {
                logger.d(TAG, "Funnel: ${event.funnelName} | Step: ${event.stepName} | Properties: ${event.properties}")
            }
        }
    }

    override fun addGlobalProperty(key: String, value: Any) {
        logger.d(TAG, "Global Property Added: $key = $value")
    }

    override fun removeGlobalProperty(key: String) {
        logger.d(TAG, "Global Property Removed: $key")
    }
}
