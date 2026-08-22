package es.joshluq.kmsafe.core.infrastructure.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import es.joshluq.foundationkit.log.LoggerKit
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

/**
 * Receiver for system geofence transition events.
 */
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var logger: LoggerKit

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            logger.e("GeofenceReceiver", "Error receiving geofence event: ${geofencingEvent.errorCode}")
            return
        }

        val transition = geofencingEvent.geofenceTransition
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()
            triggeringGeofences.forEach { geofence ->
                logger.d("GeofenceReceiver", "Entered geofence: ${geofence.requestId}")
                // Fase 3.2: Trigger Activity Recognition to confirm stay
                // For now, we'll log it. Notification logic will come in the next step.
            }
        }
    }
}
