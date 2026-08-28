package es.joshluq.kmsafe.infrastructure.repository.tracking

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import es.joshluq.foundationkit.log.LoggerKit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager responsible for registering and unregistering for activity transitions.
 *
 * The `ActivityTransitionReceiver` class name is passed as a fully-qualified string to
 * avoid a direct class reference dependency from `:core:infrastructure` to `:app`.
 * The receiver is registered in the `:app` AndroidManifest and resolved at runtime.
 */
@Singleton
class AutoTrackingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: LoggerKit
) {
    companion object {
        // Fully-qualified name of the BroadcastReceiver registered in :app/AndroidManifest.xml
        private const val ACTIVITY_TRANSITION_RECEIVER =
            "es.joshluq.kmsafe.data.location.ActivityTransitionReceiver"
    }

    private val activityRecognitionClient = ActivityRecognition.getClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(ACTIVITY_TRANSITION_RECEIVER)
        intent.setClassName(context.packageName, ACTIVITY_TRANSITION_RECEIVER)
        
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    /**
     * Registers for In-Vehicle enter and exit transitions.
     */
    @SuppressLint("MissingPermission")
    fun startAutoTracking() {
        logger.d("AutoTrackingManager", "Registering for activity transitions...")
        val transitions = listOf(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        val request = ActivityTransitionRequest(transitions)

        activityRecognitionClient.requestActivityTransitionUpdates(request, pendingIntent)
            .addOnSuccessListener {
                logger.i("AutoTrackingManager", "Successfully REGISTERED for auto-tracking")
            }
            .addOnFailureListener { e ->
                logger.e("AutoTrackingManager", "Failed to register for auto-tracking", e)
            }
    }

    /**
     * Unregisters from activity transitions.
     */
    @SuppressLint("MissingPermission")
    fun stopAutoTracking() {
        logger.d("AutoTrackingManager", "Unregistering from activity transitions...")
        activityRecognitionClient.removeActivityTransitionUpdates(pendingIntent)
            .addOnSuccessListener {
                logger.i("AutoTrackingManager", "Successfully UNREGISTERED from auto-tracking")
            }
            .addOnFailureListener { e ->
                logger.e("AutoTrackingManager", "Failed to unregister from auto-tracking", e)
            }
    }
}
