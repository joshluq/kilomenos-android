package es.joshluq.kmsafe.data.location

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import dagger.hilt.android.AndroidEntryPoint
import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.BuildConfig
import es.joshluq.kmsafe.MainActivity
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject
    lateinit var trackingRepository: TrackingRepository

    @Inject
    lateinit var logger: LoggerKit

    @Inject
    lateinit var analytics: AnalyticskitManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null

    companion object {
        private const val CHANNEL_ID = "location_tracking_channel_${BuildConfig.FLAVOR}"
        private const val NOTIFICATION_ID = 1001
        
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        // BUSINESS RULE: To avoid false positives (e.g. gym, walking, GPS drift)
        // We only count distance if the precision is high and there is a minimum speed.
        private const val MIN_SPEED_THRESHOLD_MPS = 1.5 // ~5.4 km/h (Walking/Driving speed)
        private const val MAX_HORIZONTAL_ACCURACY_METERS = 30.0 // Acceptable GPS precision
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        observeDistance()
    }

    private fun observeDistance() {
        trackingRepository.currentDistanceMeters
            .onEach { distance ->
                updateNotification(distance)
            }
            .launchIn(serviceScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.d("LocationService", "onStartCommand received with action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        serviceScope.launch {
            // Check if already tracking in repository to avoid resetting distance to 0
            val isAlreadyTracking = trackingRepository.isTracking.first()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(0.0),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification(0.0))
            }

            if (isAlreadyTracking) {
                logger.d("LocationService", "Service started but already tracking in repository. Skipping re-initialization.")
            } else {
                trackingRepository.startTracking()
            }

            val locationRequest = LocationRequest.Builder(
                PRIORITY_HIGH_ACCURACY,
                3000L
            ).setMinUpdateIntervalMillis(2000L).build()

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                logger.e("LocationService", "Permission missing for tracking", e)
                stopSelf()
            }
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            val location = result.lastLocation ?: return

            // SECURITY CONTROL: Prevent GPS Spoofing (Mock Locations)
            val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                location.isMock
            } else {
                @Suppress("DEPRECATION")
                location.isFromMockProvider
            }

            if (isMock) {
                logger.w("LocationService", "Fake location detected. Ignoring update.")
                analytics.track(AnalyticsEvent.Custom("security_gps_spoofing_detected"))
                return
            }

            // FILTER 1: Accuracy check. Indoors (gym) GPS accuracy is usually poor (> 50m).
            if (location.hasAccuracy() && location.accuracy > MAX_HORIZONTAL_ACCURACY_METERS) {
                logger.d("LocationService", "Low accuracy: ${location.accuracy}m. Skipping update.")
                return
            }

            // FILTER 2: Speed check. Prevent distance accumulation when stationary or moving very slowly.
            // If speed is below threshold, we update 'lastLocation' to keep a fresh reference but don't add distance.
            if (location.hasSpeed() && location.speed < MIN_SPEED_THRESHOLD_MPS) {
                lastLocation = location
                return
            }

            lastLocation?.let { last ->
                val distance = last.distanceTo(location)
                if (distance > 1.0) { // Ignore micro-movements
                    serviceScope.launch {
                        trackingRepository.updateDistance(distance.toDouble())
                    }
                }
            }
            lastLocation = location
        }
    }

    private fun stopTracking() {
        logger.i("LocationService", "Stopping tracking process...")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        
        serviceScope.launch {
            logger.d("LocationService", "Calling repository stopTracking")
            trackingRepository.stopTracking()
            logger.d("LocationService", "Repository stopTracking call finished")
            
            // Move cleanup inside the scope to ensure order
            stopForeground(STOP_FOREGROUND_REMOVE)
            logger.d("LocationService", "Foreground removed, calling stopSelf")
            stopSelf()
        }
    }

    private fun createNotification(distanceMeters: Double): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val kms = distanceMeters / 1000.0
        val contentText = getString(R.string.tracking_notification_content, kms)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use app icon
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground, // Replace with appropriate stop icon if available
                getString(R.string.tracking_card_stop_action),
                stopPendingIntent
            )
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification(distanceMeters: Double) {
        val notification = createNotification(distanceMeters)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        } else {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.tracking_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
