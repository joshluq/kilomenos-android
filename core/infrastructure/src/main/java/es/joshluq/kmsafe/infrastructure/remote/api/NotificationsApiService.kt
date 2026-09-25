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

/**
 * Retrofit service definition for Supabase v1 notifications endpoints (KILOMENOS-9).
 */
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
