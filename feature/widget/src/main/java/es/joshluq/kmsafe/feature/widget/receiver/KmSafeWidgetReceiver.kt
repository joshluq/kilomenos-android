package es.joshluq.kmsafe.feature.widget.receiver

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dagger.hilt.android.AndroidEntryPoint
import es.joshluq.kmsafe.feature.widget.manager.WidgetUpdateManager
import es.joshluq.kmsafe.feature.widget.ui.KmSafeGlanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AppWidget receiver for KmSafe Home Screen Widget managed by Jetpack Glance.
 */
@AndroidEntryPoint
class KmSafeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KmSafeGlanceWidget()

    @Inject
    lateinit var widgetUpdateManager: WidgetUpdateManager

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            CoroutineScope(Dispatchers.Default).launch {
                widgetUpdateManager.updateWidget()
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "es.joshluq.kmsafe.ACTION_UPDATE_WIDGET"
    }
}
