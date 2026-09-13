package es.joshluq.kmsafe.core.tracking.diagnostic

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import es.joshluq.kmsafe.core.tracking.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diagnostic & Observability engine for field testing auto-tracking and hardware telemetry.
 *
 * Provides real-time status updates via sticky diagnostic notifications in the status bar
 * for immediate driver feedback during test runs.
 */
object TrackingDiagnostics {

    private const val CHANNEL_DIAGNOSTIC_ID = "tracking_diagnostic_channel"
    private const val NOTIFICATION_ID_DIAGNOSTIC = 2099
    private const val NOTIFICATION_ID_ERROR_ALERT = 2100

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    /**
     * Updates the persistent diagnostic notification with real-time telemetry state.
     *
     * @param context Android context.
     * @param stage Current lifecycle stage (e.g. "BT_CONNECTED", "SERVICE_ARMED", "TRACKING_ACTIVE").
     * @param details Human-readable telemetry details (e.g. MAC, speed, accuracy).
     */
    @SuppressLint("MissingPermission")
    fun updateStatus(
        context: Context,
        stage: String,
        details: String
    ) {
        val timestamp = timeFormat.format(Date())
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_DIAGNOSTIC_ID,
                "KmSafe Telemetry Diagnostics",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live auto-tracking state notifications for field testing"
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_DIAGNOSTIC_ID)
            .setSmallIcon(R.drawable.ic_stat_tracking_live)
            .setColor(0xFF00B0F0.toInt())
            .setContentTitle("🚗 KmSafe Auto-Tracking Monitor")
            .setContentText("[$timestamp] $stage")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("[$timestamp] State: $stage\n$details")
            )
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID_DIAGNOSTIC, notification)
        } catch (_: Exception) {
            // Guard against SecurityException or system notify errors
        }
    }

    /**
     * Reports an error/warning event to a high-priority diagnostic alert notification.
     */
    @SuppressLint("MissingPermission")
    fun recordError(
        context: Context,
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) {
        val timestamp = timeFormat.format(Date())
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_DIAGNOSTIC_ID,
                "KmSafe Telemetry Diagnostics",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Live auto-tracking state notifications for field testing"
            }
            notificationManager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        val alertNotification = NotificationCompat.Builder(context, CHANNEL_DIAGNOSTIC_ID)
            .setSmallIcon(R.drawable.ic_stat_tracking_live)
            .setColor(0xFFE53935.toInt()) // Red
            .setContentTitle("🚨 KmSafe Auto-Tracking Alert ($tag)")
            .setContentText("[$timestamp] $message")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("[$timestamp] Tag: $tag\nError: $message\nException: ${throwable?.javaClass?.simpleName ?: "None"}")
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID_ERROR_ALERT, alertNotification)
        } catch (_: Exception) {
            // Silent fallback
        }
    }

    /**
     * Clears persistent diagnostic notifications.
     */
    fun clear(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(NOTIFICATION_ID_DIAGNOSTIC)
        } catch (_: Exception) {
            // Silent fallback
        }
    }
}
