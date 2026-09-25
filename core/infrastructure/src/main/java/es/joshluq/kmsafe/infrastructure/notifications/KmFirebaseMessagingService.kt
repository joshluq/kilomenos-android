package es.joshluq.kmsafe.infrastructure.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationPriority
import es.joshluq.kmsafe.domain.model.NotificationStatus
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.domain.usecase.PublishNotificationUseCase
import es.joshluq.kmsafe.domain.usecase.SyncEntitlementsFromPushUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        serviceScope.launch {
            // 1. Reactive Entitlements Sync trigger
            if (data.containsKey("subscription_level") ||
                data["action"] == "SYNC_ENTITLEMENTS" ||
                data["action_code"] == "REFRESH_ENTITLEMENTS"
            ) {
                syncEntitlementsFromPushUseCase(SyncEntitlementsFromPushUseCase.Input(data))
            }

            // 2. Visible message payload
            val notificationPayload = remoteMessage.notification
            if (notificationPayload != null || data.containsKey("title")) {
                val title = notificationPayload?.title ?: data["title"] ?: "Notificación"
                val body = notificationPayload?.body ?: data["body"] ?: ""
                val topicStr = data["topic"]?.uppercase()
                val topic = runCatching { NotificationTopic.valueOf(topicStr ?: "") }
                    .getOrDefault(NotificationTopic.SYSTEM)
                val priorityStr = data["priority"]?.uppercase()
                val priority = runCatching { NotificationPriority.valueOf(priorityStr ?: "") }
                    .getOrDefault(NotificationPriority.INFO)
                val deepLinkUri = data["deep_link"] ?: data["deepLinkUri"]

                val notifId = data["id"]?.takeIf { es.joshluq.kmsafe.infrastructure.repository.NotificationRepositoryImpl.isCanonicalUuid(it) }
                    ?: data["notification_id"]?.takeIf { es.joshluq.kmsafe.infrastructure.repository.NotificationRepositoryImpl.isCanonicalUuid(it) }
                    ?: java.util.UUID.randomUUID().toString()

                val notif = Notification(
                    id = notifId,
                    topic = topic,
                    title = title,
                    body = body,
                    priority = priority,
                    status = NotificationStatus.UNREAD,
                    deepLinkUri = deepLinkUri,
                    timestampMillis = remoteMessage.sentTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
                    actionLabel = data["action_label"],
                    origin = "REMOTE",
                    syncStatus = "SYNCED"
                )
                publishNotificationUseCase(PublishNotificationUseCase.Input(notif))
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
}
