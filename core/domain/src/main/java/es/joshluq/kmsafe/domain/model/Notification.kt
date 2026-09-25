package es.joshluq.kmsafe.domain.model

/**
 * Topic or domain origin of a notification.
 */
enum class NotificationTopic {
    SUBSCRIPTION,
    PROJECTION,
    FLEET,
    SYSTEM
}

/**
 * Visual and operational priority for the notification pill and list.
 */
enum class NotificationPriority {
    CRITICAL,
    WARNING,
    INFO
}

/**
 * Read and lifecycle state of a notification.
 */
enum class NotificationStatus {
    UNREAD,
    READ,
    ARCHIVED
}

/**
 * Immutable domain entity representing a system, functional, or remote notification.
 */
data class Notification(
    val id: String,
    val topic: NotificationTopic,
    val title: String,
    val body: String,
    val priority: NotificationPriority,
    val status: NotificationStatus = NotificationStatus.UNREAD,
    val deepLinkUri: String? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
    val actionLabel: String? = null,
    val userId: String = "",
    val origin: String = "LOCAL",
    val isRead: Boolean = (status == NotificationStatus.READ),
    val readAt: Long? = null,
    val createdAt: Long = timestampMillis,
    val syncStatus: String = "PENDING",
    val data: Map<String, Any>? = null
)

/**
 * Exception thrown when accessing features restricted to Premium/Pro subscribers.
 */
class PremiumRequiredException(message: String = "PREMIUM_REQUIRED") : Exception(message)

private const val KEY_CONTRACT_ID = "contract_id"

/**
 * Returns the associated contract/vehicle ID if present in payload data.
 */
val Notification.contractId: String?
    get() = data?.get(KEY_CONTRACT_ID) as? String

/**
 * Checks if the notification is targetable for the currently active vehicle.
 */
fun Notification.isForVehicle(activeVehicleId: String?): Boolean {
    val targetId = contractId
    return targetId == null || activeVehicleId == null || targetId == activeVehicleId
}