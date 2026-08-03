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

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return

        val result = ActivityTransitionResult.extractResult(intent) ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            // Check Premium Status first
            val output = isUserPremiumUseCase(IsUserPremiumUseCase.Input).first()
            if (output !is IsUserPremiumUseCase.Output.Success || !output.isPremium) {
                logger.d("ActivityReceiver", "Ignored: User is not premium.")
                return@launch
            }

            for (event in result.transitionEvents) {
                when (event.activityType) {
                    DetectedActivity.IN_VEHICLE -> handleVehicleTransition(context, event.transitionType)
                }
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
