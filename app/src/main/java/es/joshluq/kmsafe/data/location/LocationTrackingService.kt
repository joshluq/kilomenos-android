package es.joshluq.kmsafe.data.location

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
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
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.kmsafe.BuildConfig
import es.joshluq.kmsafe.MainActivity
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.domain.di.CheckFeatureAccess
import es.joshluq.kmsafe.domain.di.GetRenting
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.GetRentingContractUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

/**
 * Foreground service responsible for tracking vehicle trips via GPS.
 *
 * It handles both manual starts from the UI and automated starts from physical activity transitions.
 * It performs validations (Premium status, Bluetooth connection) before activating GPS updates.
 */
@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject
    lateinit var trackingRepository: TrackingRepository

    @Inject
    @CheckFeatureAccess
    lateinit var checkFeatureAccessUseCase:
        @JvmSuppressWildcards FlowUseCase<CheckFeatureAccessUseCase.Input, CheckFeatureAccessUseCase.Output>

    @Inject
    @GetRenting
    lateinit var getRentingContractUseCase:
        @JvmSuppressWildcards FlowUseCase<GetRentingContractUseCase.Input, GetRentingContractUseCase.Output>

    @Inject
    lateinit var logger: LoggerKit

    @Inject
    lateinit var analytics: AnalyticskitManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var distanceJob: kotlinx.coroutines.Job? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null

    companion object {
        private const val CHANNEL_ID = "location_tracking_channel_${BuildConfig.FLAVOR}"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        // BUSINESS RULE: To avoid false positives (e.g. drift when stationary)
        private const val MIN_SPEED_THRESHOLD_MPS = 1.5 // ~5.4 km/h
        private const val MAX_HORIZONTAL_ACCURACY_METERS = 30.0
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        observeDistance()
    }

    private fun observeDistance() {
        distanceJob?.cancel()
        distanceJob = trackingRepository.currentDistanceMeters
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
        logger.d("LocationService", "onStartCommand received. Action: ${intent?.action}")

        // 1. Handle explicit UI actions
        when (intent?.action) {
            ACTION_START -> {
                startTracking()
                return START_STICKY
            }
            ACTION_STOP -> {
                stopTracking()
                return START_NOT_STICKY
            }
        }

        // 2. Handle Intelligent Transitions from ActivityTransitionReceiver
        val transitionResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("EXTRA_TRANSITION_RESULT", ActivityTransitionResult::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra("EXTRA_TRANSITION_RESULT")
        }

        if (transitionResult != null) {
            logger.i("LocationService", "ActivityTransitionResult received. Processing events...")
            transitionResult.transitionEvents.forEach { event ->
                if (event.activityType == DetectedActivity.IN_VEHICLE) {
                    processTransition(event.transitionType)
                }
            }
        }

        return START_STICKY
    }

    private fun processTransition(type: Int) {
        if (type == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
            startAutoValidationAndTracking()
        } else if (type == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
            logger.i("LocationService", "IN_VEHICLE EXIT detected. Stopping service.")
            stopTracking()
        }
    }

    private fun startAutoValidationAndTracking() {
        serviceScope.launch {
            // A. Show "Validation" notification immediately to comply with Android background rules
            showValidationNotification()

            try {
                // B. Run business validations
                logger.d("LocationService", "Starting autostart validation flow...")

                // 1. Check Access
                val access = checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.AUTO_TRACKING)).first()
                if (access !is CheckFeatureAccessUseCase.Output.Success || !access.isGranted) {
                    logger.w("LocationService", "Validation failed: User has no Premium access.")
                    stopSelf()
                    return@launch
                }

                // 2. Check if already tracking
                if (trackingRepository.isTracking.first()) {
                    logger.d("LocationService", "Already tracking. Validation aborted.")
                    return@launch
                }

                // 3. Check Bluetooth if linked
                val contractOutput = getRentingContractUseCase(GetRentingContractUseCase.Input).first {
                    it !is GetRentingContractUseCase.Output.Progress
                }
                if (contractOutput is GetRentingContractUseCase.Output.Success) {
                    val mac = contractOutput.contract.bluetoothDeviceAddress
                    if (mac != null) {
                        if (!isBluetoothDeviceConnected(this@LocationTrackingService, mac)) {
                            logger.i("LocationService", "Validation failed: Vehicle Bluetooth ($mac) not found.")
                            stopSelf()
                            return@launch
                        }
                    }
                } else {
                    logger.w("LocationService", "Validation failed: No active vehicle selected.")
                    stopSelf()
                    return@launch
                }

                // C. Validations passed! Start GPS capture
                logger.i("LocationService", "VALIDATIONS PASSED. Switching to active tracking.")
                startTracking()
            } catch (e: Exception) {
                logger.e("LocationService", "Error during autostart validation", e)
                stopSelf()
            }
        }
    }

    private fun showValidationNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.tracking_validation_content))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun isBluetoothDeviceConnected(context: Context, macAddress: String): Boolean {
        // 1. Permission Guard for Android 12+ (API 31)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permission = android.Manifest.permission.BLUETOOTH_CONNECT
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                logger.w("LocationService", "Bluetooth validation skipped: BLUETOOTH_CONNECT permission missing.")
                analytics.track(AnalyticsEvent.Custom("tracking_bt_validation_skipped_no_permission"))
                return true // Fallback: prioritize tracking over validation
            }
        }

        val normalizedTarget = macAddress.replace(":", "").uppercase().trim()
        val bluetoothManager = context.getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: return false
        if (!adapter.isEnabled) return false

        return suspendCancellableCoroutine { continuation ->
            val profileListener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    try {
                        val connectedDevices = proxy.connectedDevices
                        val isTargetConnected = connectedDevices.any {
                            it.address.replace(":", "").uppercase().trim() == normalizedTarget
                        }
                        if (isTargetConnected && !continuation.isCompleted) continuation.resume(true)
                    } catch (e: SecurityException) {
                        // Double-safety: Handle unexpected late permission revocation or system inconsistencies
                        logger.e("LocationService", "SecurityException during Bluetooth check fallback applied", e)
                        if (!continuation.isCompleted) continuation.resume(true)
                    } catch (e: Exception) {
                        logger.e("LocationService", "Unexpected error during Bluetooth check", e)
                        if (!continuation.isCompleted) continuation.resume(true)
                    } finally {
                        adapter.closeProfileProxy(profile, proxy)
                    }
                }
                override fun onServiceDisconnected(profile: Int) {}
            }
            adapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
            adapter.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)

            serviceScope.launch {
                delay(3000.milliseconds)
                if (!continuation.isCompleted) continuation.resume(false)
            }
        }
    }

    private fun startTracking() {
        logger.i("LocationService", "startTracking initiated")
        serviceScope.launch {
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

            if (!isAlreadyTracking) {
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

            // SECURITY CONTROL: Prevent GPS Spoofing
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

            // Accuracy & Speed Filters
            if (location.hasAccuracy() && location.accuracy > MAX_HORIZONTAL_ACCURACY_METERS) return
            if (location.hasSpeed() && location.speed < MIN_SPEED_THRESHOLD_MPS) {
                lastLocation = location
                return
            }

            lastLocation?.let { last ->
                val distance = last.distanceTo(location)
                if (distance > 20.0) {
                    serviceScope.launch {
                        trackingRepository.updateTracking(
                            distanceMeters = distance.toDouble(),
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    }
                    lastLocation = location
                } else if (distance > 1.0) {
                    serviceScope.launch {
                        trackingRepository.updateTracking(
                            distanceMeters = distance.toDouble()
                        )
                    }
                    lastLocation = location
                }
            }
            if (lastLocation == null) lastLocation = location
        }
    }

    private fun stopTracking() {
        logger.i("LocationService", "Stopping tracking process...")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        distanceJob?.cancel()

        serviceScope.launch {
            trackingRepository.stopTracking()
            stopForeground(STOP_FOREGROUND_REMOVE)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
            stopSelf()
        }
    }

    private fun createNotification(distanceMeters: Double): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val kms = distanceMeters / 1000.0
        val contentText = getString(R.string.tracking_notification_content, kms)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
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
