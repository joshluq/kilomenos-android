package es.joshluq.kmsafe.infrastructure.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface responsible for initializing and managing system notification channels.
 */
interface NotificationChannelManager {
    /**
     * Initializes all required notification channels on Android 8.0 (API 26) or higher.
     */
    fun initializeChannels()

    /**
     * Returns the channel ID designated for subscription and billing alerts.
     */
    fun getSubscriptionChannelId(): String
}

@Singleton
class NotificationChannelManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationChannelManager {

    companion object {
        const val CHANNEL_SUBSCRIPTION_ALERTS = "subscription_alerts"
        const val CHANNEL_SUBSCRIPTION_NAME = "Alertas de Suscripción"
        const val CHANNEL_SUBSCRIPTION_DESC = "Avisos sobre cambios en tu estado de suscripción y renovaciones"
    }

    internal var sdkVersion: Int = Build.VERSION.SDK_INT

    internal var channelCreator: (NotificationManager, String, String, Int, String) -> Unit = { manager, id, name, importance, desc ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, name, importance).apply {
                description = desc
                enableLights(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    override fun initializeChannels() {
        if (sdkVersion >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
            channelCreator(
                notificationManager,
                CHANNEL_SUBSCRIPTION_ALERTS,
                CHANNEL_SUBSCRIPTION_NAME,
                NotificationManager.IMPORTANCE_HIGH,
                CHANNEL_SUBSCRIPTION_DESC
            )
        }
    }

    override fun getSubscriptionChannelId(): String = CHANNEL_SUBSCRIPTION_ALERTS
}
