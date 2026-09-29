package es.joshluq.kmsafe.infrastructure.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationPriority
import es.joshluq.kmsafe.domain.model.NotificationStatus
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.domain.usecase.PublishNotificationUseCase
import es.joshluq.kmsafe.domain.usecase.RegisterDeviceTokenUseCase
import es.joshluq.kmsafe.domain.usecase.SyncEntitlementsFromPushUseCase
import es.joshluq.kmsafe.infrastructure.repository.NotificationRepositoryImpl.Companion.isCanonicalUuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import androidx.core.net.toUri

/**
 * Firebase Cloud Messaging service for handling remote push notifications
 * and executing background entitlement synchronizations.
 */
@AndroidEntryPoint
class KmFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var syncEntitlementsFromPushUseCase: SyncEntitlementsFromPushUseCase

    @Inject
    lateinit var publishNotificationUseCase: PublishNotificationUseCase

    @Inject
    lateinit var registerDeviceTokenUseCase: RegisterDeviceTokenUseCase

    @Inject
    lateinit var channelManager: NotificationChannelManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        serviceScope.launch {
            // 1. Reactive Entitlements Sync trigger
            var downgradeNotification: Notification? = null
            if (data.containsKey("subscription_level") ||
                data["action"] == "SYNC_ENTITLEMENTS" ||
                data["action_code"] == "REFRESH_ENTITLEMENTS" ||
                data["event_type"] == "SUBSCRIPTION_DOWNGRADED"
            ) {
                val syncResult = syncEntitlementsFromPushUseCase(SyncEntitlementsFromPushUseCase.Input(data))
                if (syncResult.isSuccess) {
                    val output = syncResult.getOrNull()
                    if (output is SyncEntitlementsFromPushUseCase.Output.Success && output.isDowngraded) {
                        downgradeNotification = output.notification
                    }
                }
            }

            // 2. Visible message payload
            val notificationPayload = remoteMessage.notification
            if (notificationPayload != null || data.containsKey("title") || data.containsKey("message") || downgradeNotification != null) {
                val notifToPost = if (downgradeNotification != null) {
                    downgradeNotification
                } else {
                    val title = notificationPayload?.title ?: data["title"] ?: "KiloMenos"
                    val body = notificationPayload?.body ?: data["message"] ?: data["body"] ?: ""
                    val topicStr = data["topic"]?.uppercase()
                    val topic = runCatching { NotificationTopic.valueOf(topicStr ?: "") }
                        .getOrDefault(NotificationTopic.SUBSCRIPTION)
                    val priorityStr = data["priority"]?.uppercase()
                    val priority = runCatching { NotificationPriority.valueOf(priorityStr ?: "") }
                        .getOrDefault(NotificationPriority.WARNING)
                    val deepLinkUri = data["deep_link"] ?: data["deepLinkUri"] ?: "kmsafe://app/notifications"

                    val notifId = data["notification_id"]?.takeIf { isCanonicalUuid(it) }
                        ?: data["id"]?.takeIf { isCanonicalUuid(it) }
                        ?: UUID.randomUUID().toString()

                    val notif = Notification(
                        id = notifId,
                        topic = topic,
                        title = title,
                        body = body,
                        priority = priority,
                        status = NotificationStatus.UNREAD,
                        deepLinkUri = deepLinkUri,
                        timestampMillis = remoteMessage.sentTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
                        actionLabel = data["action_label"] ?: "Ver",
                        origin = "REMOTE",
                        syncStatus = "SYNCED"
                    )

                    publishNotificationUseCase(PublishNotificationUseCase.Input(notif))
                    notif
                }

                // Post system alert with NotificationCompat
                postSystemNotification(notifToPost)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun postSystemNotification(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val channelId = channelManager.getSubscriptionChannelId()
        val intent = Intent(
            Intent.ACTION_VIEW,
            (notification.deepLinkUri ?: "kmsafe://app/notifications").toUri()
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val safeNotificationId = notification.id.hashCode() and 0x7FFFFFFF

        val pendingIntent = PendingIntent.getActivity(
            this,
            safeNotificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val brandIconRes = resources.getIdentifier("ic_stat_kmsafe_brand", "drawable", packageName)
            .takeIf { it != 0 } ?: applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_dialog_alert

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(brandIconRes)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(this).notify(safeNotificationId, builder.build())
        } catch (_: SecurityException) {
            // Handled when POST_NOTIFICATIONS is not granted in Android 13+
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            registerDeviceTokenUseCase(RegisterDeviceTokenUseCase.Input(token))
        }
    }
}
