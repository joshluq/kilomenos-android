package es.joshluq.kmsafe.core.infrastructure.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.core.domain.usecase.SyncStationGeofencesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver triggered when the device boots or when the app package is replaced/updated.
 * Restores Google Play Services geofences for favorite stations since the operating system
 * purges all registered geofences on system reboot.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var syncStationGeofencesUseCase: SyncStationGeofencesUseCase

    @Inject
    lateinit var logger: LoggerKit

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            logger.d("BootReceiver", "Boot or package replacement detected ($action), syncing station geofences")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    syncStationGeofencesUseCase(SyncStationGeofencesUseCase.Input).collect()
                    logger.d("BootReceiver", "Station geofences successfully restored after boot")
                } catch (e: Exception) {
                    logger.e("BootReceiver", "Failed to restore station geofences on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
