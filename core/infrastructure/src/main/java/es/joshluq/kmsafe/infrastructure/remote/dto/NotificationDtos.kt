package es.joshluq.kmsafe.infrastructure.remote.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotificationsListResponse(
    @JsonProperty("success") val success: Boolean = true,
    @JsonProperty("data") val data: NotificationListData? = null,
    @JsonProperty("error") val error: String? = null,
    @JsonProperty("message") val message: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotificationListData(
    @JsonProperty("items") val items: List<NotificationRemoteDto> = emptyList(),
    @JsonProperty("total") val total: Int = 0,
    @JsonProperty("unread_count") val unreadCount: Int = 0
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotificationRemoteDto(
    @JsonProperty("id") val id: String,
    @JsonProperty("user_id") val userId: String? = null,
    @JsonProperty("origin") val origin: String = "REMOTE",
    @JsonProperty("topic") val topic: String = "SYSTEM",
    @JsonProperty("title") val title: String = "",
    @JsonProperty("body") val body: String = "",
    @JsonProperty("data") val data: Map<String, Any>? = null,
    @JsonProperty("deep_link") val deepLink: String? = null,
    @JsonProperty("is_read") val isRead: Boolean = false,
    @JsonProperty("read_at") val readAt: String? = null,
    @JsonProperty("created_at") val createdAt: String? = null,
    @JsonProperty("updated_at") val updatedAt: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotificationSyncRequest(
    @JsonProperty("notifications") val notifications: List<NotificationSyncItemDto>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotificationSyncItemDto(
    @JsonProperty("id") val id: String,
    @JsonProperty("origin") val origin: String = "LOCAL",
    @JsonProperty("topic") val topic: String,
    @JsonProperty("title") val title: String,
    @JsonProperty("body") val body: String,
    @JsonProperty("data") val data: Map<String, Any>? = null,
    @JsonProperty("deep_link") val deepLink: String? = null,
    @JsonProperty("is_read") val isRead: Boolean = false,
    @JsonProperty("read_at") val readAt: String? = null,
    @JsonProperty("created_at") val createdAt: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotificationSyncResponse(
    @JsonProperty("success") val success: Boolean = true,
    @JsonProperty("data") val data: NotificationSyncData? = null,
    @JsonProperty("error") val error: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotificationSyncData(
    @JsonProperty("synced_count") val syncedCount: Int = 0
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotificationReadResponse(
    @JsonProperty("success") val success: Boolean = true,
    @JsonProperty("data") val data: NotificationReadData? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotificationReadData(
    @JsonProperty("id") val id: String,
    @JsonProperty("is_read") val isRead: Boolean = true,
    @JsonProperty("read_at") val readAt: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotificationReadAllResponse(
    @JsonProperty("success") val success: Boolean = true,
    @JsonProperty("data") val data: NotificationReadAllData? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NotificationReadAllData(
    @JsonProperty("updated_count") val updatedCount: Int = 0
)
