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
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.GetRentingContractUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receiver that listens for Bluetooth connection events.
 * Suggests linking the device to the user if they are Premium and haven't set up a car Bluetooth yet.
 */
@AndroidEntryPoint
class BluetoothConnectionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var logger: LoggerKit

    @Inject
    lateinit var checkFeatureAccessUseCase: CheckFeatureAccessUseCase

    @Inject
    lateinit var getRentingContractUseCase: GetRentingContractUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) return

        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        } ?: return

        val pendingResult = goAsync()

        scope.launch {
            try {
                // 1. Check if user has Premium Access
                val accessOutput = checkFeatureAccessUseCase(
                    CheckFeatureAccessUseCase.Input(Feature.AUTO_TRACKING)
                ).first()
                val isPremium = (accessOutput is CheckFeatureAccessUseCase.Output.Success) && accessOutput.isGranted

                if (!isPremium) return@launch

                // 2. Check if current contract has NO bluetooth device registered
                val contractOutput = getRentingContractUseCase(GetRentingContractUseCase.Input).first()
                if (contractOutput !is GetRentingContractUseCase.Output.Success) return@launch

                val contract = contractOutput.contract
                if (contract.bluetoothDeviceAddress == null) {
                    logger.i("BluetoothReceiver", "New connection detected: ${device.address}. Suggesting link.")
                    showSuggestionNotification(context, device)
                }
            } catch (e: Exception) {
                logger.e("BluetoothReceiver", "Error processing bluetooth event", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showSuggestionNotification(context: Context, device: BluetoothDevice) {
        val channelId = "bluetooth_suggestion_channel"
        val notificationId = 2002

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
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

        val notification = NotificationCompat.Builder(context, channelId)
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

        notificationManager.notify(notificationId, notification)
    }
}
