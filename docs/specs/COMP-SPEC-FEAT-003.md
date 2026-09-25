# Component Interface Specification: Integración Local-First de Notificaciones y Sincronización Remota

**Feature ID**: FEAT-003 (Jira: KILOMENOS-10)  
**Component Identifiers**: NotificationsApiService, NotificationEntity, NotificationDao, SyncNotificationsWorker, NotificationRepository, NotificationsListViewModel, NotificationsListScreen  
**Packages**:
- `es.joshluq.kmsafe.domain.model` & `es.joshluq.kmsafe.domain.repository` (`:core:domain`)
- `es.joshluq.kmsafe.infrastructure.remote.api` & `es.joshluq.kmsafe.infrastructure.remote.dto` (`:core:infrastructure`)
- `es.joshluq.kmsafe.infrastructure.local.entity` & `es.joshluq.kmsafe.infrastructure.local.dao` (`:core:infrastructure`)
- `es.joshluq.kmsafe.infrastructure.worker` (`:core:infrastructure`)
- `es.joshluq.kmsafe.feature.notifications.ui.list` (`:feature:notifications`)  
**Target Modules**: `:feature:notifications`, `:core:domain`, `:core:infrastructure`, `:core:network`  
**Architecture Pattern**: Pure MVI + Local-First Offline Sync + 4-Layer Jetpack Compose  
**Status**: APPROVED  

---

## 1. Remote API Service Contracts (`:core:infrastructure`)

### 1.1 Retrofit Interface (`NotificationsApiService.kt`)
```kotlin
package es.joshluq.kmsafe.infrastructure.remote.api

import es.joshluq.kmsafe.infrastructure.remote.dto.NotificationReadAllResponse
import es.joshluq.kmsafe.infrastructure.remote.dto.NotificationReadResponse
import es.joshluq.kmsafe.infrastructure.remote.dto.NotificationSyncRequest
import es.joshluq.kmsafe.infrastructure.remote.dto.NotificationSyncResponse
import es.joshluq.kmsafe.infrastructure.remote.dto.NotificationsListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationsApiService {

    @GET("v1/notifications")
    suspend fun getNotifications(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("unread_only") unreadOnly: Boolean? = null,
        @Query("topic") topic: String? = null
    ): Response<NotificationsListResponse>

    @POST("v1/notifications/sync")
    suspend fun syncNotifications(
        @Body body: NotificationSyncRequest
    ): Response<NotificationSyncResponse>

    @PATCH("v1/notifications/{id}/read")
    suspend fun markAsRead(
        @Path("id") id: String
    ): Response<NotificationReadResponse>

    @POST("v1/notifications/read-all")
    suspend fun markAllAsRead(
        @Body body: Map<String, String> = emptyMap()
    ): Response<NotificationReadAllResponse>

    @DELETE("v1/notifications/{id}")
    suspend fun deleteNotification(
        @Path("id") id: String
    ): Response<Map<String, Any>>
}
```

### 1.2 Data Transfer Objects (`NotificationDtos.kt`)
```kotlin
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
    @JsonProperty("topic") val topic: String,
    @JsonProperty("title") val title: String,
    @JsonProperty("body") val body: String,
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
    @JsonProperty("is_read") val isRead: Boolean,
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
```

---

## 2. Local Database Entities & DAO (`:core:infrastructure`)

### 2.1 Room Entity (`NotificationEntity.kt`)
```kotlin
package es.joshluq.kmsafe.infrastructure.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
```

### 2.2 Room DAO (`NotificationDao.kt`)
```kotlin
@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestampMillis DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE topic = :topic ORDER BY timestampMillis DESC")
    fun getNotificationsByTopic(topic: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE status = 'UNREAD' OR isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    suspend fun getNotificationById(id: String): NotificationEntity?

    @Query("UPDATE notifications SET status = 'READ', isRead = 1, readAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    suspend fun markAsRead(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE notifications SET status = 'READ', isRead = 1, readAt = :now, syncStatus = 'PENDING' WHERE status = 'UNREAD' OR isRead = 0")
    suspend fun markAllAsRead(now: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(notifications: List<NotificationEntity>)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM notifications WHERE syncStatus = 'PENDING' ORDER BY timestampMillis ASC")
    suspend fun getPendingSyncNotifications(): List<NotificationEntity>

    @Query("UPDATE notifications SET syncStatus = :status WHERE id IN (:ids)")
    suspend fun updateSyncStatus(ids: List<String>, status: String)
}
```

---

## 3. WorkManager Background Worker (`SyncNotificationsWorker.kt`)

```kotlin
package es.joshluq.kmsafe.infrastructure.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import es.joshluq.kmsafe.domain.repository.NotificationRepository

@HiltWorker
class SyncNotificationsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationRepository: NotificationRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val syncResult = notificationRepository.syncPending()
            if (syncResult) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "SyncNotificationsWork"
    }
}
```

---

## 4. Domain & Presentation Layer (`:feature:notifications`)

### 4.1 NotificationsListUiState
```kotlin
package es.joshluq.kmsafe.feature.notifications.ui.list

import androidx.compose.runtime.Immutable
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationTopic

@Immutable
data class NotificationsListUiState(
    val notifications: List<Notification> = emptyList(),
    val selectedFilter: NotificationTopic? = null,
    val unreadCount: Int = 0,
    val isSyncing: Boolean = false,
    val isPremiumRequiredBannerVisible: Boolean = false,
    val errorMessage: String? = null
)
```

### 4.2 NotificationsListUiAction
```kotlin
package es.joshluq.kmsafe.feature.notifications.ui.list

import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationTopic

sealed interface NotificationsListUiAction {
    data class OnNotificationClicked(val notification: Notification) : NotificationsListUiAction
    data class OnMarkAsReadClicked(val notificationId: String) : NotificationsListUiAction
    data object OnMarkAllAsReadClicked : NotificationsListUiAction
    data class OnDeleteNotificationClicked(val notificationId: String) : NotificationsListUiAction
    data class OnFilterSelected(val topic: NotificationTopic?) : NotificationsListUiAction
    data object OnDismissPremiumBannerClicked : NotificationsListUiAction
    data object OnUpgradeToProClicked : NotificationsListUiAction
    data object OnRefreshRequested : NotificationsListUiAction
}
```
