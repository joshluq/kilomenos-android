package es.joshluq.kmsafe.feature.notifications.ui.mapper

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.feature.notifications.R

data class NotificationLocalizedTexts(
    val title: TextProvider,
    val body: TextProvider,
    val actionLabel: TextProvider?,
    val topicLabel: TextProvider
)

object NotificationTextResolver {

    fun resolve(notification: Notification): NotificationLocalizedTexts {
        return when (notification.topic) {
            NotificationTopic.PROJECTION -> resolveProjection(notification)
            NotificationTopic.SYSTEM -> resolveSystem(notification)
            NotificationTopic.SUBSCRIPTION -> resolveSubscription(notification)
            NotificationTopic.FLEET -> resolveFleet(notification)
        }
    }

    private fun resolveProjection(notification: Notification): NotificationLocalizedTexts {
        val isOverLimit = notification.data?.get("is_over_limit") as? Boolean ?: true
        val excessKm = notification.data?.get("excess_km")?.toString()
            ?: notification.data?.get("current_km")?.toString()
            ?: "0"

        return if (isOverLimit) {
            NotificationLocalizedTexts(
                title = TextProvider.Resource(R.string.notification_projection_excess_title),
                body = TextProvider.Resource(R.string.notification_projection_excess_body, excessKm),
                actionLabel = TextProvider.Resource(R.string.notification_projection_action),
                topicLabel = TextProvider.Resource(R.string.notification_topic_projection)
            )
        } else {
            NotificationLocalizedTexts(
                title = TextProvider.Resource(R.string.notification_projection_safe_title),
                body = TextProvider.Resource(R.string.notification_projection_safe_body),
                actionLabel = TextProvider.Resource(R.string.notification_projection_action),
                topicLabel = TextProvider.Resource(R.string.notification_topic_projection)
            )
        }
    }

    private fun resolveSystem(notification: Notification): NotificationLocalizedTexts {
        val semanticKey = notification.data?.get("deduplication_key") as? String ?: ""
        return if (semanticKey.contains("bt_missing")) {
            NotificationLocalizedTexts(
                title = TextProvider.Resource(R.string.notification_system_bt_missing_title),
                body = TextProvider.Resource(R.string.notification_system_bt_missing_body),
                actionLabel = TextProvider.Resource(R.string.notification_system_bt_missing_action),
                topicLabel = TextProvider.Resource(R.string.notification_topic_system)
            )
        } else {
            fallback(notification, R.string.notification_topic_system)
        }
    }

    private fun resolveSubscription(notification: Notification): NotificationLocalizedTexts {
        return NotificationLocalizedTexts(
            title = TextProvider.Resource(R.string.notification_subscription_title),
            body = TextProvider.Resource(R.string.notification_subscription_body),
            actionLabel = TextProvider.Resource(R.string.notification_subscription_action),
            topicLabel = TextProvider.Resource(R.string.notification_topic_subscription)
        )
    }

    private fun resolveFleet(notification: Notification): NotificationLocalizedTexts {
        return NotificationLocalizedTexts(
            title = TextProvider.Resource(R.string.notification_fleet_title),
            body = TextProvider.Resource(R.string.notification_fleet_body),
            actionLabel = null,
            topicLabel = TextProvider.Resource(R.string.notification_topic_fleet)
        )
    }

    private fun fallback(notification: Notification, defaultTopicRes: Int): NotificationLocalizedTexts {
        val titleText = if (notification.title.isNotBlank()) {
            TextProvider.Dynamic(notification.title)
        } else {
            TextProvider.Resource(defaultTopicRes)
        }
        val bodyText = if (notification.body.isNotBlank()) {
            TextProvider.Dynamic(notification.body)
        } else {
            TextProvider.Resource(defaultTopicRes)
        }
        return NotificationLocalizedTexts(
            title = titleText,
            body = bodyText,
            actionLabel = notification.actionLabel?.takeIf { it.isNotBlank() }?.let { TextProvider.Dynamic(it) },
            topicLabel = TextProvider.Resource(defaultTopicRes)
        )
    }
}
