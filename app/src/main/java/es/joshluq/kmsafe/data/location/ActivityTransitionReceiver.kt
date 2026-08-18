package es.joshluq.kmsafe.data.location

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.android.AndroidEntryPoint
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.kmsafe.domain.di.CheckFeatureAccess
import es.joshluq.kmsafe.domain.di.GetRenting
import es.joshluq.kmsafe.domain.di.ObserveTrackingState
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.GetRentingContractUseCase
import es.joshluq.kmsafe.domain.usecase.ObserveTrackingStateUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

/**
 * Receiver that handles transitions between physical activities (e.g., STILL to IN_VEHICLE).
 */
@AndroidEntryPoint
class ActivityTransitionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var logger: LoggerKit

    @Inject
    @CheckFeatureAccess
    lateinit var checkFeatureAccessUseCase:
        @JvmSuppressWildcards FlowUseCase<CheckFeatureAccessUseCase.Input, CheckFeatureAccessUseCase.Output>

    @Inject
    @GetRenting
    lateinit var getRentingContractUseCase:
        @JvmSuppressWildcards FlowUseCase<GetRentingContractUseCase.Input, GetRentingContractUseCase.Output>

    @Inject
    @ObserveTrackingState
    lateinit var observeTrackingStateUseCase:
        @JvmSuppressWildcards FlowUseCase<ObserveTrackingStateUseCase.Input, ObserveTrackingStateUseCase.Output>

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

                // SECURITY GUARD: If already tracking or waiting for confirmation, don't restart.
                // This prevents the user from being stuck in "Active Tracking" mode in the gym.
                val trackingOutput = observeTrackingStateUseCase(ObserveTrackingStateUseCase.Input).first()
                if (trackingOutput is ObserveTrackingStateUseCase.Output.Success) {
                    if (trackingOutput.isTracking || trackingOutput.trackedDistance > 0.0) {
                        logger.d("ActivityReceiver", "Tracking active or confirmation pending. Ignoring ENTER event.")
                        return@launch
                    }
                }

                // Bluetooth Validation: If a device is paired, check if it's connected
                val contractOutput = getRentingContractUseCase(GetRentingContractUseCase.Input).first()
                val contract = if (contractOutput is GetRentingContractUseCase.Output.Success) contractOutput.contract else null

                val bluetoothMac = contract?.bluetoothDeviceAddress
                if (bluetoothMac != null && !isBluetoothDeviceConnected(context, bluetoothMac)) {
                    logger.i(
                        "ActivityReceiver",
                        "In-Vehicle detected but car Bluetooth ($bluetoothMac) not connected. Ignoring."
                    )
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

    private suspend fun isBluetoothDeviceConnected(context: Context, macAddress: String): Boolean {
        val startTime = System.currentTimeMillis()
        logger.d("ActivityReceiver", "Checking car Bluetooth ($macAddress)")

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: return false

        if (!adapter.isEnabled) {
            logger.w("ActivityReceiver", "Bluetooth is disabled")
            return false
        }

        return suspendCancellableCoroutine { continuation ->
            val profileListener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    val isMacConnected = proxy.connectedDevices.any {
                        it.address.equals(macAddress, ignoreCase = true)
                    }

                    if (isMacConnected && !continuation.isCompleted) {
                        val duration = System.currentTimeMillis() - startTime
                        logger.i("ActivityReceiver", "Bluetooth CONFIRMED ($macAddress) in ${duration}ms")
                        continuation.resume(true)
                    }

                    adapter.closeProfileProxy(profile, proxy)
                }

                override fun onServiceDisconnected(profile: Int) {}
            }

            adapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
            adapter.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)

            scope.launch {
                delay(2000.milliseconds)
                if (!continuation.isCompleted) {
                    logger.w(
                        "ActivityReceiver",
                        "Bluetooth check TIMEOUT after 2000ms. Device ($macAddress) not found."
                    )
                    continuation.resume(false)
                }
            }
        }
    }
}
