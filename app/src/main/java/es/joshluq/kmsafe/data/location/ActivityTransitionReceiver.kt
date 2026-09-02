package es.joshluq.kmsafe.data.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.ActivityTransitionResult
import dagger.hilt.android.AndroidEntryPoint
import es.joshluq.foundationkit.log.LoggerKit
import javax.inject.Inject

/**
 * Receiver that handles transitions between physical activities (e.g., STILL to IN_VEHICLE).
 *
 * This receiver is woken up by Google Play Services when a physical activity change is detected.
 * It immediately forwards the event to [LocationTrackingService] to handle business logic
 * and start GPS tracking if necessary.
 */
@AndroidEntryPoint
class ActivityTransitionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var logger: LoggerKit

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val action = intent.action ?: ""

        logger.d("ActivityReceiver", "onReceive: $action")

        // Standard Activity Transition Action (from Google Play Services)
        val isTransitionAction = action == "es.joshluq.kmsafe.data.location.ActivityTransitionReceiver"

        if (isTransitionAction && ActivityTransitionResult.hasResult(intent)) {
            logger.i("ActivityReceiver", "Transition result received. Forwarding to Service...")

            val result = ActivityTransitionResult.extractResult(intent)
            val serviceIntent = Intent(context, LocationTrackingService::class.java).apply {
                putExtra("EXTRA_TRANSITION_RESULT", result)
            }

            startTrackingService(context, serviceIntent)
        }

        pendingResult.finish()
    }

    private fun startTrackingService(context: Context, intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            logger.e("ActivityReceiver", "FAILED TO START SERVICE: ${e.message}")
        }
    }
}
