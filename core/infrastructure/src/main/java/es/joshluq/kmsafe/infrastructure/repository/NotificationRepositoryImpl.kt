package es.joshluq.kmsafe.infrastructure.repository

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.domain.model.PremiumRequiredException
import es.joshluq.kmsafe.domain.repository.NotificationRepository
import es.joshluq.kmsafe.infrastructure.local.dao.NotificationDao
import es.joshluq.kmsafe.infrastructure.local.entity.NotificationEntity
import es.joshluq.kmsafe.infrastructure.local.entity.toDomain
import es.joshluq.kmsafe.infrastructure.local.entity.toEntity
import es.joshluq.kmsafe.infrastructure.mapper.toIsoString
import es.joshluq.kmsafe.infrastructure.remote.api.NotificationsApiService
import es.joshluq.kmsafe.infrastructure.remote.dto.NotificationSyncItemDto
import es.joshluq.kmsafe.infrastructure.remote.dto.NotificationSyncRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-First implementation of [NotificationRepository] with Room and Supabase v1 REST API.
 */
@Singleton
class NotificationRepositoryImpl(
    private val notificationDao: NotificationDao,
    private val notificationsApiService: NotificationsApiService,
    private val logger: LoggerKit,
    private val coroutineScope: CoroutineScope
) : NotificationRepository {

    @Inject
    constructor(
        notificationDao: NotificationDao,
        notificationsApiService: NotificationsApiService,
        logger: LoggerKit
    ) : this(
        notificationDao = notificationDao,
        notificationsApiService = notificationsApiService,
        logger = logger,
        coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    )

    override fun observeNotifications(topic: NotificationTopic?): Flow<List<Notification>> {
        return if (topic != null) {
            notificationDao.getNotificationsByTopic(topic.name).map { list -> list.map { it.toDomain() } }
        } else {
            notificationDao.getAllNotifications().map { list -> list.map { it.toDomain() } }
        }
    }

    override fun observeUnreadCount(): Flow<Int> {
        return notificationDao.getUnreadCount()
    }

    override suspend fun getNotificationById(id: String): Notification? {
        return notificationDao.getNotificationById(id)?.toDomain()
    }

    override suspend fun getNotificationBySemanticKey(semanticKey: String): Notification? {
        return notificationDao.findBySemanticKey(semanticKey)?.toDomain()
    }

    override suspend fun markAsRead(id: String) {
        var targetId = id
        val existing = notificationDao.getNotificationById(id)
        if (existing == null) {
            val bySemantic = notificationDao.findActiveBySemanticKey(id) ?: notificationDao.findBySemanticKey(id)
            if (bySemantic != null) {
                targetId = bySemantic.id
            }
        }
        // Optimistic UI update in Room
        notificationDao.markAsRead(targetId)
        // Background sync to backend only if targetId is canonical UUID
        if (isCanonicalUuid(targetId)) {
            runCatching {
                val response = notificationsApiService.markAsRead(targetId)
                if (response.isSuccessful) {
                    notificationDao.updateSyncStatus(listOf(targetId), "SYNCED")
                } else {
                    logger.w("NotificationRepository", "markAsRead remote call failed: HTTP ${response.code()}")
                }
            }.onFailure { e ->
                logger.e("NotificationRepository", "markAsRead exception", e)
            }
        }
    }

    override suspend fun markAllAsRead() {
        // Optimistic UI update in Room
        notificationDao.markAllAsRead()
        // Background sync to backend
        runCatching {
            val response = notificationsApiService.markAllAsRead()
            if (!response.isSuccessful) {
                logger.w("NotificationRepository", "markAllAsRead remote call failed: HTTP ${response.code()}")
            }
        }.onFailure { e ->
            logger.e("NotificationRepository", "markAllAsRead exception", e)
        }
    }

    override suspend fun insertOrUpdate(notification: Notification) {
        val semanticKey = notification.data?.get("projection_key") as? String
            ?: notification.data?.get("deduplication_key") as? String

        if (semanticKey != null) {
            val existing = notificationDao.findActiveBySemanticKey(semanticKey)
                ?: notificationDao.findBySemanticKey(semanticKey)
            if (existing != null) {
                // Deduplicate: preserve original UUID and update fields
                val updated = notification.copy(
                    id = existing.id,
                    timestampMillis = existing.timestampMillis,
                    createdAt = existing.createdAt
                )
                notificationDao.insertOrUpdate(updated.toEntity())
                triggerOpportunisticSync()
                return
            }
        }
        notificationDao.insertOrUpdate(notification.toEntity())
        triggerOpportunisticSync()
    }

    private fun triggerOpportunisticSync() {
        coroutineScope.launch {
            runCatching {
                syncPending()
            }
        }
    }

    override suspend fun delete(id: String) {
        var targetId = id
        val existing = notificationDao.getNotificationById(id)
        if (existing == null) {
            val bySemantic = notificationDao.findActiveBySemanticKey(id) ?: notificationDao.findBySemanticKey(id)
            if (bySemantic != null) {
                targetId = bySemantic.id
            }
        }
        // Optimistic UI update in Room
        notificationDao.deleteById(targetId)
        // Background sync to backend only if canonical UUID
        if (isCanonicalUuid(targetId)) {
            runCatching {
                val response = notificationsApiService.deleteNotification(targetId)
                if (!response.isSuccessful) {
                    logger.w("NotificationRepository", "deleteNotification remote call failed: HTTP ${response.code()}")
                }
            }.onFailure { e ->
                logger.e("NotificationRepository", "deleteNotification exception", e)
            }
        }
    }

    override suspend fun syncPending(): Boolean {
        val pending = notificationDao.getPendingSyncNotifications()
        if (pending.isEmpty()) return true

        // Partition valid UUIDs from legacy non-UUID IDs
        val (validPending, legacyPending) = pending.partition { isCanonicalUuid(it.id) }
        if (legacyPending.isNotEmpty()) {
            notificationDao.updateSyncStatus(legacyPending.map { it.id }, "SYNCED")
        }

        if (validPending.isEmpty()) return true

        logger.d("NotificationRepository", "Starting syncPending for ${validPending.size} pending notifications")

        return try {
            val syncItems = validPending.map { entity ->
                NotificationSyncItemDto(
                    id = entity.id,
                    origin = entity.origin,
                    topic = entity.topic.lowercase(),
                    title = entity.title,
                    body = entity.body,
                    data = entity.toDomain().data,
                    deepLink = entity.deepLinkUri,
                    isRead = entity.isRead,
                    readAt = entity.readAt?.toIsoString(),
                    createdAt = entity.createdAt.toIsoString()
                )
            }
            val response = notificationsApiService.syncNotifications(NotificationSyncRequest(syncItems))
            if (response.isSuccessful && response.body()?.success == true) {
                notificationDao.updateSyncStatus(validPending.map { it.id }, "SYNCED")
                logger.i("NotificationRepository", "Successfully synced ${validPending.size} notifications to Supabase")
                true
            } else {
                val code = response.code()
                val errBody = response.errorBody()?.string().orEmpty()
                if (code == 403 || errBody.contains("PREMIUM_REQUIRED")) {
                    logger.w("NotificationRepository", "Sync rejected: HTTP 403 PREMIUM_REQUIRED. User lacks cloud sync entitlement.")
                } else {
                    logger.e("NotificationRepository", "Sync failed: HTTP $code - $errBody")
                }
                false
            }
        } catch (e: Exception) {
            logger.e("NotificationRepository", "Exception during syncPending", e)
            false
        }
    }

    companion object {
        private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

        fun isCanonicalUuid(id: String): Boolean = UUID_REGEX.matches(id)
    }

    override suspend fun fetchRemoteNotifications(): Result<Int> {
        return try {
            val response = notificationsApiService.getNotifications(limit = 50)
            if (response.code() == 403) {
                return Result.failure(PremiumRequiredException())
            }
            if (!response.isSuccessful) {
                val errBody = response.errorBody()?.string().orEmpty()
                if (errBody.contains("PREMIUM_REQUIRED")) {
                    return Result.failure(PremiumRequiredException())
                }
                return Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }

            val remoteItems = response.body()?.data?.items.orEmpty()
            val entities = remoteItems.map { dto ->
                NotificationEntity(
                    id = dto.id,
                    userId = dto.userId.orEmpty(),
                    origin = dto.origin,
                    topic = dto.topic,
                    title = dto.title,
                    body = dto.body,
                    priority = "INFO",
                    status = if (dto.isRead) "READ" else "UNREAD",
                    deepLinkUri = dto.deepLink,
                    isRead = dto.isRead,
                    readAt = dto.readAt?.toLongOrNull(),
                    createdAt = dto.createdAt?.toLongOrNull() ?: System.currentTimeMillis(),
                    timestampMillis = dto.createdAt?.toLongOrNull() ?: System.currentTimeMillis(),
                    syncStatus = "SYNCED"
                )
            }
            if (entities.isNotEmpty()) {
                notificationDao.insertOrUpdateAll(entities)
            }
            Result.success(remoteItems.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
