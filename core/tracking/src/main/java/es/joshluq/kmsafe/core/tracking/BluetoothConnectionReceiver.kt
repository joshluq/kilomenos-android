package es.joshluq.kmsafe.core.tracking

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.GetPreferencesUseCase
import es.joshluq.kmsafe.domain.usecase.GetRentingContractUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receiver that listens for Bluetooth hardware connection and disconnection events.
 *
 * Provides immediate feedback via silent local notifications when a linked vehicle connects,
 * enables fast-path trip finalization upon vehicle disconnection, and suggests linking new devices
 * when no vehicle Bluetooth is registered.
 */
@AndroidEntryPoint
class BluetoothConnectionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var logger: LoggerKit

    @Inject
    lateinit var checkFeatureAccessUseCase: CheckFeatureAccessUseCase

    @Inject
    lateinit var getRentingContractUseCase: GetRentingContractUseCase

    @Inject
    lateinit var getPreferencesUseCase: GetPreferencesUseCase

    @Inject
    lateinit var trackingRepository: TrackingRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val CHANNEL_SUGGESTION_ID = "bluetooth_suggestion_channel"
        private const val CHANNEL_CONNECTED_ID = "bluetooth_feedback_channel"
        private const val NOTIFICATION_ID_SUGGESTION = 2002
        private const val NOTIFICATION_ID_CONNECTED = 2003
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != BluetoothDevice.ACTION_ACL_CONNECTED && action != BluetoothDevice.ACTION_ACL_DISCONNECTED) {
            return
        }

        val device = intent.getBluetoothDeviceExtra() ?: return
        val pendingResult = goAsync()

        scope.launch {
            try {
                when (action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> handleBluetoothConnected(context, device)
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> handleBluetoothDisconnected(context, device)
                }
            } catch (e: Exception) {
                logger.e("BluetoothReceiver", "Error processing bluetooth event: $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleBluetoothConnected(context: Context, device: BluetoothDevice) {
        // 1. Check if user has Premium Access
        val accessOutput = checkFeatureAccessUseCase(
            CheckFeatureAccessUseCase.Input(Feature.AUTO_TRACKING)
        ).first()
        val isPremium = (accessOutput is CheckFeatureAccessUseCase.Output.Success) && accessOutput.isGranted

        if (!isPremium) return

        // 2. Fetch active contract
        val contractOutput = getRentingContractUseCase(GetRentingContractUseCase.Input).first()
        if (contractOutput !is GetRentingContractUseCase.Output.Success) return

        val contract = contractOutput.contract
        val deviceAddress = device.address ?: return
        val normalizedDeviceMac = normalizeAddress(deviceAddress)

        val contractBluetoothMac = contract.bluetoothDeviceAddress
        if (contractBluetoothMac == null) {
            // Case A: No bluetooth linked to this contract yet -> suggest linking
            logger.i("BluetoothReceiver", "New connection detected: $deviceAddress. Suggesting link.")
            showSuggestionNotification(context, device)
        } else {
            // Case B: Contract has linked MAC -> verify if it matches
            val normalizedContractMac = normalizeAddress(contractBluetoothMac)
            if (normalizedDeviceMac == normalizedContractMac) {
                val prefsOutput = getPreferencesUseCase(GetPreferencesUseCase.Input).first()
                val isAutoTrackingEnabled = (prefsOutput is GetPreferencesUseCase.Output.Success) &&
                    prefsOutput.preferences.autoTrackingEnabled

                if (isAutoTrackingEnabled) {
                    logger.i("BluetoothReceiver", "Vehicle Bluetooth connected: ${contract.vehicleName}. Showing feedback notification.")
                    showConnectedNotification(context, contract.vehicleName)
                }
            }
        }
    }

    private suspend fun handleBluetoothDisconnected(context: Context, device: BluetoothDevice) {
        val contractOutput = getRentingContractUseCase(GetRentingContractUseCase.Input).first()
        if (contractOutput !is GetRentingContractUseCase.Output.Success) return

        val contract = contractOutput.contract
        val contractMac = contract.bluetoothDeviceAddress?.let { normalizeAddress(it) } ?: return
        val deviceAddress = device.address ?: return
        val normalizedDeviceMac = normalizeAddress(deviceAddress)

        if (normalizedDeviceMac == contractMac) {
            logger.i("BluetoothReceiver", "Vehicle Bluetooth disconnected: ${contract.vehicleName}. Dismissing feedback notification.")
            dismissConnectedNotification(context)

            // Fast-path: Conclude trip tracking immediately instead of waiting for Activity Recognition delay
            val isTracking = trackingRepository.isTracking.first()
            if (isTracking) {
                logger.i("BluetoothReceiver", "Active trip detected during vehicle disconnect. Stopping tracking service immediately.")
                val stopIntent = Intent(context, LocationTrackingService::class.java).apply {
                    action = LocationTrackingService.ACTION_STOP
                }
                context.startService(stopIntent)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showConnectedNotification(context: Context, vehicleName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_CONNECTED_ID,
                context.getString(R.string.tracking_bluetooth_connected_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.tracking_bluetooth_connected_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        } ?: Intent()

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CONNECTED_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle(context.getString(R.string.tracking_bluetooth_connected_title, vehicleName))
            .setContentText(context.getString(R.string.tracking_bluetooth_connected_desc))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_CONNECTED, notification)
    }

    private fun dismissConnectedNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_CONNECTED)
    }

    @SuppressLint("MissingPermission")
    private fun showSuggestionNotification(context: Context, device: BluetoothDevice) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_SUGGESTION_ID,
                context.getString(R.string.tracking_bluetooth_suggestion_title),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "renting_details")
            putExtra("vehicle_id", "active")
        } ?: Intent()

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SUGGESTION_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle(context.getString(R.string.tracking_bluetooth_suggestion_title))
            .setContentText(context.getString(R.string.tracking_bluetooth_suggestion_desc))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                0,
                context.getString(R.string.tracking_bluetooth_suggestion_button),
                pendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID_SUGGESTION, notification)
    }

    private fun normalizeAddress(address: String): String {
        return address.replace(":", "").uppercase().trim()
    }

    private fun Intent.getBluetoothDeviceExtra(): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }
}
