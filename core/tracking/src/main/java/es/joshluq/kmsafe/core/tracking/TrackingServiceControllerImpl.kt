package es.joshluq.kmsafe.core.tracking

import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.service.TrackingServiceController
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [TrackingServiceController] that starts and stops
 * [LocationTrackingService] using type-safe Kotlin class references.
 */
@Singleton
class TrackingServiceControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: LoggerKit
) : TrackingServiceController {

    override fun startTrackingService() {
        logger.d("TrackingServiceController", "Starting LocationTrackingService via type-safe intent")
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    override fun stopTrackingService() {
        logger.d("TrackingServiceController", "Stopping LocationTrackingService via type-safe intent")
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP
        }
        context.startService(intent)
    }
}
