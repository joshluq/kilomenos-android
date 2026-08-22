package es.joshluq.kmsafe.data.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import es.joshluq.kmsafe.MainActivity
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.core.domain.service.StationNotificationService
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.ui.navigation.DeepLinkConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [StationNotificationService] using Android System Notifications.
 */
@Singleton
class StationNotificationServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : StationNotificationService {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "station_proximity_channel"
    private val notificationId = 3001

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.notification_channel_stations_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_stations_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    override fun showStationProximityPrompt(station: ServiceStation) {
        val baseUri = "${DeepLinkConfig.BASE_URL}/expenses"
        
        val fullRefuelUri = "$baseUri?stationId=${station.id}&autoOpenAdd=true&priceReportMode=false".toUri()
        val priceUpdateUri = "$baseUri?stationId=${station.id}&autoOpenAdd=true&priceReportMode=true".toUri()

        val fullRefuelIntent = Intent(Intent.ACTION_VIEW, fullRefuelUri, context, MainActivity::class.java)
        val priceUpdateIntent = Intent(Intent.ACTION_VIEW, priceUpdateUri, context, MainActivity::class.java)

        val fullRefuelPendingIntent = PendingIntent.getActivity(
            context,
            station.id.hashCode() + 1,
            fullRefuelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priceUpdatePendingIntent = PendingIntent.getActivity(
            context,
            station.id.hashCode() + 2,
            priceUpdateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_station_proximity_title, station.name))
            .setContentText(context.getString(R.string.notification_station_proximity_desc))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(fullRefuelPendingIntent)
            .addAction(
                0,
                context.getString(R.string.notification_station_proximity_action_price),
                priceUpdatePendingIntent
            )
            .addAction(
                0,
                context.getString(R.string.notification_station_proximity_action_refuel),
                fullRefuelPendingIntent
            )
            .build()

        notificationManager.notify(notificationId, notification)
    }

    override fun cancelProximityPrompt() {
        notificationManager.cancel(notificationId)
    }
}
