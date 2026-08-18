package es.joshluq.kmsafe.infrastructure.analytics

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import es.joshluq.analyticskit.data.provider.AnalyticsProvider
import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import javax.inject.Inject

/**
 * An [AnalyticsProvider] that routes events to Google Firebase Analytics.
 */
class FirebaseAnalyticsProvider @Inject constructor(
    @ApplicationContext context: Context
) : AnalyticsProvider {

    @SuppressLint("MissingPermission")
    private val firebase = FirebaseAnalytics.getInstance(context)

    override val key: String = "FirebaseProvider"

    override suspend fun track(event: AnalyticsEvent) {
        when (event) {
            is AnalyticsEvent.Custom -> {
                firebase.logEvent(event.name, event.properties.toBundle())
            }
            is AnalyticsEvent.ScreenView -> {
                val bundle = Bundle().apply {
                    putString(FirebaseAnalytics.Param.SCREEN_NAME, event.screenName)
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, event.screenClass ?: "Compose")
                }
                firebase.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            }
            is AnalyticsEvent.FunnelStep -> {
                val bundle = event.properties.toBundle().apply {
                    putString("funnel_name", event.funnelName)
                    putString("step_name", event.stepName)
                }
                firebase.logEvent("funnel_step", bundle)
            }
        }
    }

    override fun addGlobalProperty(key: String, value: Any) {
        firebase.setUserProperty(key, value.toString())
    }

    override fun removeGlobalProperty(key: String) {
        firebase.setUserProperty(key, null)
    }

    private fun Map<String, Any>.toBundle(): Bundle {
        val bundle = Bundle()
        forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        return bundle
    }
}
