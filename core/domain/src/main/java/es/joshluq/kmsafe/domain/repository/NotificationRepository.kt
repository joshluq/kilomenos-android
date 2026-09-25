package es.joshluq.kmsafe.domain.repository

import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationTopic
import kotlinx.coroutines.flow.Flow

/**
 * Agnostic domain repository interface for notifications.
 * Allows functional modules to emit and observe notifications without UI coupling.
 */
interface NotificationRepository {
    fun observeNotifications(topic: NotificationTopic? = null): Flow<List<Notification>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun getNotificationById(id: String): Notification?
    suspend fun getNotificationBySemanticKey(semanticKey: String): Notification?
    suspend fun markAsRead(id: String)
    suspend fun markAllAsRead()
    suspend fun insertOrUpdate(notification: Notification)
    suspend fun delete(id: String)
    suspend fun syncPending(): Boolean
    suspend fun fetchRemoteNotifications(): Result<Int>
}
