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
import es.joshluq.kmsafe.di.CheckFeatureAccess
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.foundationkit.usecase.FlowUseCase
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
    @CheckFeatureAccess
    lateinit var checkFeatureAccessUseCase: @JvmSuppressWildcards FlowUseCase<CheckFeatureAccessUseCase.Input, CheckFeatureAccessUseCase.Output>

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

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
                // Check Access for Auto-Tracking feature
                val output = checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.AUTO_TRACKING)).first()
                val isGranted = (output is CheckFeatureAccessUseCase.Output.Success) && output.isGranted

                if (!isGranted) {
                    return@launch
                }

                for (event in result.transitionEvents) {
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
                serviceIntent.action = LocationTrackingService.ACTION_START
                startTrackingService(context, serviceIntent)
            }
            ActivityTransition.ACTIVITY_TRANSITION_EXIT -> {
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
