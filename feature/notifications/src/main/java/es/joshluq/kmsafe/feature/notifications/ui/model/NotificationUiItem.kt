package es.joshluq.kmsafe.feature.notifications.ui.model

import androidx.compose.runtime.Immutable
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationPriority
import es.joshluq.kmsafe.domain.model.NotificationStatus
import es.joshluq.kmsafe.domain.model.NotificationTopic

/**
 * Presentation UI Model representing a notification ready for declarative Compose rendering.
 * All user-facing texts are represented as [TextProvider] with zero Android Context dependency.
 */
@Immutable
data class NotificationUiItem(
    val id: String,
    val notification: Notification,
    val title: TextProvider,
    val body: TextProvider,
    val actionLabel: TextProvider?,
    val topicLabel: TextProvider,
    val topic: NotificationTopic,
    val priority: NotificationPriority,
    val status: NotificationStatus,
    val isRead: Boolean,
    val timestampMillis: Long,
    val vehicleId: String?,
    val vehicleName: String?,
    val isForActiveVehicle: Boolean
)
