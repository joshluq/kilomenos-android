package es.joshluq.kmsafe.data.location

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
 */
@Singleton
class AutoTrackingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: LoggerKit
) {

    private val activityRecognitionClient = ActivityRecognition.getClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, ActivityTransitionReceiver::class.java)
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
            .addOnFailureListener { e ->
                logger.e("AutoTrackingManager", "Failed to register for auto-tracking", e)
            }
    }

    /**
     * Unregisters from activity transitions.
     */
    @SuppressLint("MissingPermission")
    fun stopAutoTracking() {
        activityRecognitionClient.removeActivityTransitionUpdates(pendingIntent)
            .addOnFailureListener { e ->
                logger.e("AutoTrackingManager", "Failed to unregister from auto-tracking", e)
            }
    }
}
