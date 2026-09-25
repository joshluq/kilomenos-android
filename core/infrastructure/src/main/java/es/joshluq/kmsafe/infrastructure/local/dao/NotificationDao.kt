package es.joshluq.kmsafe.infrastructure.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import es.joshluq.kmsafe.infrastructure.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for local notifications with local-first sync support.
 */
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

    @Query("SELECT * FROM notifications WHERE dataJson LIKE '%' || :semanticKey || '%' LIMIT 1")
    suspend fun findBySemanticKey(semanticKey: String): NotificationEntity?

    @Query("SELECT * FROM notifications WHERE dataJson LIKE '%' || :semanticKey || '%' AND (status = 'UNREAD' OR isRead = 0) LIMIT 1")
    suspend fun findActiveBySemanticKey(semanticKey: String): NotificationEntity?

    @Query("UPDATE notifications SET syncStatus = :status WHERE id IN (:ids)")
    suspend fun updateSyncStatus(ids: List<String>, status: String)
}
