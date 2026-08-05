package es.joshluq.kmsafe.data.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.ActivityTransition
import dagger.hilt.android.AndroidEntryPoint
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.usecase.IsUserPremiumUseCase
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.kmsafe.di.IsUserPremium
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receiver that handles transitions between physical activities (e.g., STILL to IN_VEHICLE).
 */
@AndroidEntryPoint
class ActivityTransitionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var logger: LoggerKit

    @Inject
    @IsUserPremium
    lateinit var isUserPremiumUseCase: @JvmSuppressWildcards FlowUseCase<IsUserPremiumUseCase.Input, IsUserPremiumUseCase.Output>

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        
        logger.i("ActivityReceiver", "onReceive triggered with action: ${intent.action}")

        if (!ActivityTransitionResult.hasResult(intent)) {
            logger.w("ActivityReceiver", "Received intent without ActivityTransitionResult")
            pendingResult.finish()
            return
        }

        val result = ActivityTransitionResult.extractResult(intent) ?: run {
            pendingResult.finish()
            return
        }
        
        scope.launch {
            try {
                // Check Premium Status first
                // Use a timeout or ensure first() returns promptly
                val output = isUserPremiumUseCase(IsUserPremiumUseCase.Input).first()
                val isPremium = (output is IsUserPremiumUseCase.Output.Success && output.isPremium)

                if (!isPremium) {
                    logger.w("ActivityReceiver", "Ignored: User is not premium.")
                    return@launch
                }

                for (event in result.transitionEvents) {
                    logger.i("ActivityReceiver", "Transition Event: Type=${event.activityType}, Transition=${event.transitionType}")
                    if (event.activityType == DetectedActivity.IN_VEHICLE) {
                        handleVehicleTransition(context, event.transitionType)
                    }
                }
            } catch (e: Exception) {
                logger.e("ActivityReceiver", "Error processing transition result", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleVehicleTransition(context: Context, transitionType: Int) {
        val serviceIntent = Intent(context, LocationTrackingService::class.java)
        
        when (transitionType) {
            ActivityTransition.ACTIVITY_TRANSITION_ENTER -> {
                logger.i("ActivityReceiver", "🚗 Entering vehicle detected. Auto-starting GPS.")
                serviceIntent.action = LocationTrackingService.ACTION_START
                startTrackingService(context, serviceIntent)
            }
            ActivityTransition.ACTIVITY_TRANSITION_EXIT -> {
                logger.i("ActivityReceiver", "🚶 Exiting vehicle detected. Stopping GPS.")
                serviceIntent.action = LocationTrackingService.ACTION_STOP
                context.startService(serviceIntent)
            }
        }
    }

    private fun startTrackingService(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
