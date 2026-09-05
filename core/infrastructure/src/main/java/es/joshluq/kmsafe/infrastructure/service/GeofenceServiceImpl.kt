package es.joshluq.kmsafe.core.infrastructure.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.core.domain.service.GeofenceService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [GeofenceService] using Google Play Services Geofencing API.
 */
@Singleton
class GeofenceServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: LoggerKit
) : GeofenceService {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    override fun registerStationGeofences(stations: List<ServiceStation>): Flow<Unit> = flow {
        if (stations.isEmpty()) {
            geofencingClient.removeGeofences(geofencePendingIntent)
            emit(Unit)
            return@flow
        }

        val geofences = stations
            .filter { it.latitude != 0.0 && it.longitude != 0.0 }
            .map { station ->
                Geofence.Builder()
                    .setRequestId(station.id)
                    .setCircularRegion(
                        station.latitude,
                        station.longitude,
                        180f // 180 meters radius
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_ENTER)
                    .setLoiteringDelay(45000) // 45 seconds stay inside geofence before triggering dwell
                    .build()
            }

        if (geofences.isEmpty()) {
            emit(Unit)
            return@flow
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL or GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        logger.d("GeofenceService", "Registering ${geofences.size} geofences")

        try {
            // Remove existing first to avoid duplicates
            geofencingClient.removeGeofences(geofencePendingIntent)

            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    logger.d("GeofenceService", "Geofences registered successfully")
                }
                .addOnFailureListener { e ->
                    logger.e("GeofenceService", "Failed to register geofences", e)
                }
        } catch (e: SecurityException) {
            logger.e("GeofenceService", "Location permission missing when adding geofences", e)
        }

        emit(Unit)
    }

    override fun clearAllGeofences(): Flow<Unit> = flow {
        logger.d("GeofenceService", "Clearing all geofences")
        try {
            geofencingClient.removeGeofences(geofencePendingIntent)
                .addOnSuccessListener {
                    logger.d("GeofenceService", "Geofences cleared successfully")
                }
                .addOnFailureListener { e ->
                    logger.e("GeofenceService", "Failed to clear geofences", e)
                }
        } catch (e: SecurityException) {
            logger.e("GeofenceService", "SecurityException clearing geofences", e)
        }
        emit(Unit)
    }
}
