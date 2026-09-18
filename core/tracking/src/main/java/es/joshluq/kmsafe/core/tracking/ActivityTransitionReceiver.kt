package es.joshluq.kmsafe.core.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.android.AndroidEntryPoint
import es.joshluq.foundationkit.log.LoggerKit
import javax.inject.Inject

/**
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

        val isTransitionAction = action == "es.joshluq.kmsafe.core.tracking.ActivityTransitionReceiver"

        if (isTransitionAction && ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            if (result != null) {
                logger.i("ActivityReceiver", "Transition result received. Analyzing events...")

                val lastRelevantEvent = result.transitionEvents
                    .filter {
                        it.activityType == DetectedActivity.IN_VEHICLE ||
                        it.activityType == DetectedActivity.WALKING ||
                        it.activityType == DetectedActivity.ON_FOOT
                    }
                    .maxByOrNull { it.elapsedRealTimeNanos }

                if (lastRelevantEvent != null) {
                    when (lastRelevantEvent.activityType) {
                        DetectedActivity.IN_VEHICLE -> {
                            when (lastRelevantEvent.transitionType) {
                                ActivityTransition.ACTIVITY_TRANSITION_ENTER -> {
                                    logger.i("ActivityReceiver", "Latest IN_VEHICLE transition is ENTER. Starting tracking service...")
                                    val serviceIntent = Intent(context, LocationTrackingService::class.java).apply {
                                        putExtra("EXTRA_TRANSITION_RESULT", result)
                                    }
                                    startTrackingService(context, serviceIntent)
                                }
                                ActivityTransition.ACTIVITY_TRANSITION_EXIT -> {
                                    logger.i("ActivityReceiver", "Latest IN_VEHICLE transition is EXIT. Stopping service via startService...")
                                    val stopIntent = Intent(context, LocationTrackingService::class.java).apply {
                                        this.action = LocationTrackingService.ACTION_STOP
                                    }
                                    try {
                                        context.startService(stopIntent)
                                    } catch (e: Exception) {
                                        logger.e("ActivityReceiver", "Failed to send stop command to service: ${e.message}", e)
                                    }
                                }
                            }
                        }
                        DetectedActivity.WALKING, DetectedActivity.ON_FOOT -> {
                            if (lastRelevantEvent.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                                val linkedMac = TrackingDeviceCache.getLinkedMac(context)
                                val isBtConnected = TrackingDeviceCache.isBluetoothConnected()
                                val activityName = if (lastRelevantEvent.activityType == DetectedActivity.WALKING) "WALKING" else "ON_FOOT"

                                if (linkedMac == null || !isBtConnected) {
                                    logger.i("ActivityReceiver", "Pedestrian transition detected ($activityName ENTER) without active vehicle Bluetooth. Stopping service...")
                                    val stopIntent = Intent(context, LocationTrackingService::class.java).apply {
                                        this.action = LocationTrackingService.ACTION_STOP
                                    }
                                    try {
                                        context.startService(stopIntent)
                                    } catch (e: Exception) {
                                        logger.e("ActivityReceiver", "Failed to send stop command for pedestrian transition: ${e.message}", e)
                                    }
                                } else {
                                    logger.d("ActivityReceiver", "Pedestrian transition ($activityName ENTER) ignored because vehicle Bluetooth is actively connected.")
                                }
                            }
                        }
                    }
                } else {
                    logger.d("ActivityReceiver", "No relevant transition in result. Ignoring.")
                }
            }
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
            logger.e("ActivityReceiver", "FAILED TO START SERVICE: ${e.message}", e)
        }
    }
}

