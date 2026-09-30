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

import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.TrackingMode
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

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

    @Inject
    lateinit var checkFeatureAccessUseCase: CheckFeatureAccessUseCase

    @Inject
    lateinit var trackingRepository: TrackingRepository

    internal var coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    internal var startServiceCommand: (context: Context, intent: Intent) -> Unit = { ctx, serviceIntent ->
        startTrackingService(ctx, serviceIntent)
    }

    internal var stopServiceCommand: (context: Context, action: String) -> Unit = { ctx, stopAction ->
        val stopIntent = Intent(ctx, LocationTrackingService::class.java).apply {
            this.action = stopAction
        }
        try {
            ctx.startService(stopIntent)
        } catch (e: Exception) {
            logger.e("ActivityReceiver", "Failed to send stop command to service: ${e.message}", e)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val action = intent.action ?: ""

        logger.d("ActivityReceiver", "onReceive: $action")

        val isTransitionAction = action == "es.joshluq.kmsafe.core.tracking.ActivityTransitionReceiver"

        if (isTransitionAction && ActivityTransitionResult.hasResult(intent)) {
            coroutineScope.launch {
                try {
                    // Check entitlement: Only Premium users are entitled to Auto-Tracking
                    val access = withTimeoutOrNull(2000L.milliseconds) {
                        checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.AUTO_TRACKING)).first()
                    }
                    val isPremium = (access is CheckFeatureAccessUseCase.Output.Success) && access.isGranted
                    if (!isPremium) {
                        logger.w("ActivityReceiver", "Activity transition received for FREE user. Discarding event.")
                        return@launch
                    }

                    // Check active trip mode: NEVER stop or interfere with a MANUAL session
                    val isTracking = trackingRepository.isTracking.first()
                    val mode = trackingRepository.trackingMode.first()

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
                                            startServiceCommand(context, serviceIntent)
                                        }
                                        ActivityTransition.ACTIVITY_TRANSITION_EXIT -> {
                                            if (isTracking && mode == TrackingMode.MANUAL) {
                                                logger.i("ActivityReceiver", "IN_VEHICLE EXIT detected but active trip is MANUAL. Stop command suppressed.")
                                            } else {
                                                logger.i("ActivityReceiver", "Latest IN_VEHICLE transition is EXIT. Stopping service via ACTION_STOP_AUTOMATIC...")
                                                stopServiceCommand(context, LocationTrackingService.ACTION_STOP_AUTOMATIC)
                                            }
                                        }
                                    }
                                }
                                DetectedActivity.WALKING, DetectedActivity.ON_FOOT -> {
                                    if (lastRelevantEvent.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                                        if (isTracking && mode == TrackingMode.MANUAL) {
                                            logger.i("ActivityReceiver", "Pedestrian transition detected but active trip is MANUAL. Stop command suppressed.")
                                        } else {
                                            val linkedMac = TrackingDeviceCache.getLinkedMac(context)
                                            val isBtConnected = TrackingDeviceCache.isBluetoothConnected()
                                            val activityName = if (lastRelevantEvent.activityType == DetectedActivity.WALKING) "WALKING" else "ON_FOOT"

                                            if (linkedMac == null || !isBtConnected) {
                                                logger.i("ActivityReceiver", "Pedestrian transition detected ($activityName ENTER) without active vehicle Bluetooth. Stopping service via ACTION_STOP_AUTOMATIC...")
                                                stopServiceCommand(context, LocationTrackingService.ACTION_STOP_AUTOMATIC)
                                            } else {
                                                logger.d("ActivityReceiver", "Pedestrian transition ($activityName ENTER) ignored because vehicle Bluetooth is actively connected.")
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            logger.d("ActivityReceiver", "No relevant transition in result. Ignoring.")
                        }
                    }
                } catch (e: Exception) {
                    logger.e("ActivityReceiver", "Error processing activity transition: ${e.message}", e)
                } finally {
                    pendingResult?.finish()
                }
            }
        } else {
            pendingResult?.finish()
        }
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

