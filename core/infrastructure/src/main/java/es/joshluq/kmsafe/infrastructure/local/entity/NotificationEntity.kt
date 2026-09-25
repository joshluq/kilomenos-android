package es.joshluq.kmsafe.infrastructure.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationPriority
import es.joshluq.kmsafe.domain.model.NotificationStatus
import es.joshluq.kmsafe.domain.model.NotificationTopic

/**
 * Room entity representing an in-app or remote notification in local storage.
 */
@Entity(
    tableName = "notifications",
    indices = [
        Index("topic"),
        Index("status"),
        Index("timestampMillis"),
        Index("syncStatus"),
        Index("userId")
    ]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val origin: String = "LOCAL",
    val topic: String,
    val title: String,
    val body: String,
    val priority: String = "INFO",
    val status: String = "UNREAD",
    val dataJson: String? = null,
    val deepLinkUri: String? = null,
    val isRead: Boolean = false,
    val readAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val timestampMillis: Long = System.currentTimeMillis(),
    val actionLabel: String? = null,
    val syncStatus: String = "PENDING"
)

private val notificationJsonMapper by lazy { ObjectMapper() }

fun parseJsonToMap(json: String?): Map<String, Any>? {
    if (json.isNullOrBlank()) return null
    return runCatching {
        notificationJsonMapper.readValue(json, object : TypeReference<Map<String, Any>>() {})
    }.getOrNull()
}

fun mapToJsonString(map: Map<String, Any>?): String? {
    if (map.isNullOrEmpty()) return null
    return runCatching {
        notificationJsonMapper.writeValueAsString(map)
    }.getOrNull()
}

/**
 * Maps [NotificationEntity] to domain [Notification].
 */
fun NotificationEntity.toDomain(): Notification = Notification(
    id = id,
    userId = userId,
    origin = origin,
    topic = runCatching { NotificationTopic.valueOf(topic) }.getOrDefault(NotificationTopic.SYSTEM),
    title = title,
    body = body,
    priority = runCatching { NotificationPriority.valueOf(priority) }.getOrDefault(NotificationPriority.INFO),
    status = if (isRead) NotificationStatus.READ else runCatching { NotificationStatus.valueOf(status) }.getOrDefault(NotificationStatus.UNREAD),
    deepLinkUri = deepLinkUri,
    isRead = isRead || status == "READ",
    readAt = readAt,
    createdAt = createdAt,
    timestampMillis = timestampMillis,
    actionLabel = actionLabel,
    syncStatus = syncStatus,
    data = parseJsonToMap(dataJson)
)

/**
 * Maps domain [Notification] to [NotificationEntity].
 */
fun Notification.toEntity(): NotificationEntity = NotificationEntity(
    id = id,
    userId = userId,
    origin = origin,
    topic = topic.name,
    title = title,
    body = body,
    priority = priority.name,
    status = if (isRead) NotificationStatus.READ.name else status.name,
    deepLinkUri = deepLinkUri,
    isRead = isRead || status == NotificationStatus.READ,
    readAt = readAt,
    createdAt = createdAt,
    timestampMillis = timestampMillis,
    actionLabel = actionLabel,
    syncStatus = syncStatus,
    dataJson = mapToJsonString(data)
)
