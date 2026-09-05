package es.joshluq.kmsafe.core.infrastructure.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.core.domain.usecase.HandleGeofenceTransitionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Receiver for system geofence transition events.
 */
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var handleGeofenceTransitionUseCase: HandleGeofenceTransitionUseCase

    @Inject
    lateinit var logger: LoggerKit

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            logger.e("GeofenceReceiver", "Error receiving geofence event: ${geofencingEvent.errorCode}")
            return
        }

        val transition = geofencingEvent.geofenceTransition
        if (transition == Geofence.GEOFENCE_TRANSITION_DWELL || transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()
            if (triggeringGeofences.isEmpty()) return

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    withTimeoutOrNull(5000L.milliseconds) {
                        triggeringGeofences.forEach { geofence ->
                            logger.d("GeofenceReceiver", "Processing geofence transition ($transition) for station: ${geofence.requestId}")
                            handleGeofenceTransitionUseCase(HandleGeofenceTransitionUseCase.Input(geofence.requestId)).collect()
                        }
                    }
                } catch (e: Exception) {
                    logger.e("GeofenceReceiver", "Error processing geofence transition", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
