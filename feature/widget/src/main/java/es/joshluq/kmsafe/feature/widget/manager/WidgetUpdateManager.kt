package es.joshluq.kmsafe.feature.widget.manager

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.feature.widget.ui.KmSafeGlanceWidget
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager responsible for invalidating and updating all active instances of KmSafeGlanceWidget
 * upon relevant domain events (e.g. odometer record saved, trip finished, sync completed).
 */
@Singleton
class WidgetUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: LoggerKit
) {
    /**
     * Re-renders all active Home Screen widgets with fresh domain metrics.
     */
    suspend fun updateWidget() {
        try {
            logger.d("WidgetUpdateManager", "Triggering widget update across all active instances")
            KmSafeGlanceWidget().updateAll(context)
        } catch (e: Exception) {
            logger.e("WidgetUpdateManager", "Failed to update widget instances", e)
        }
    }
}
