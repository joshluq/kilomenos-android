package es.joshluq.kmsafe.core.analytics.impl

import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.kmsafe.core.analytics.AnalyticsTracker
import es.joshluq.kmsafe.core.analytics.model.KmsafeAnalyticsEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsTrackerImpl @Inject constructor(
    private val analyticsManager: AnalyticskitManager
) : AnalyticsTracker {

    override fun track(event: KmsafeAnalyticsEvent) {
        analyticsManager.track(AnalyticsEvent.Custom(event.name, event.properties))
    }

    override fun trackScreen(screenName: String, screenClass: String?) {
        analyticsManager.track(AnalyticsEvent.ScreenView(screenName, screenClass))
    }

    override fun setUserProperty(key: String, value: String?) {
        if (value != null) {
            analyticsManager.addGlobalProperty(key, value)
        } else {
            analyticsManager.removeGlobalProperty(key)
        }
    }
}
