package es.joshluq.kmsafe.core.analytics.fake

import es.joshluq.kmsafe.core.analytics.AnalyticsTracker
import es.joshluq.kmsafe.core.analytics.model.KmsafeAnalyticsEvent

/**
 * In-memory test fake for [AnalyticsTracker].
 * Used across unit tests without MockK boilerplate.
 */
class FakeAnalyticsTracker : AnalyticsTracker {
    val trackedEvents = mutableListOf<KmsafeAnalyticsEvent>()
    val trackedScreens = mutableListOf<String>()
    val userProperties = mutableMapOf<String, String>()

    override fun track(event: KmsafeAnalyticsEvent) {
        trackedEvents.add(event)
    }

    override fun trackScreen(screenName: String, screenClass: String?) {
        trackedScreens.add(screenName)
    }

    override fun setUserProperty(key: String, value: String?) {
        if (value != null) {
            userProperties[key] = value
        } else {
            userProperties.remove(key)
        }
    }

    fun clear() {
        trackedEvents.clear()
        trackedScreens.clear()
        userProperties.clear()
    }
}
