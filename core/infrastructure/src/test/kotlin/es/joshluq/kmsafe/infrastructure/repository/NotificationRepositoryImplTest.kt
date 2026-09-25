package es.joshluq.kmsafe.infrastructure.repository

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationPriority
import es.joshluq.kmsafe.domain.model.NotificationStatus
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.domain.model.PremiumRequiredException
import es.joshluq.kmsafe.infrastructure.local.dao.NotificationDao
import es.joshluq.kmsafe.infrastructure.local.entity.NotificationEntity
import es.joshluq.kmsafe.infrastructure.local.entity.toEntity
import es.joshluq.kmsafe.infrastructure.remote.api.NotificationsApiService
import es.joshluq.kmsafe.infrastructure.remote.dto.NotificationListData
import es.joshluq.kmsafe.infrastructure.remote.dto.NotificationReadData
import es.joshluq.kmsafe.infrastructure.remote.dto.NotificationReadResponse
import es.joshluq.kmsafe.infrastructure.remote.dto.NotificationRemoteDto
import es.joshluq.kmsafe.infrastructure.remote.dto.NotificationSyncData
import es.joshluq.kmsafe.infrastructure.remote.dto.NotificationSyncResponse
import es.joshluq.kmsafe.infrastructure.remote.dto.NotificationsListResponse
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class NotificationRepositoryImplTest {

    private val notificationDao: NotificationDao = mockk(relaxed = true)
    private val notificationsApiService: NotificationsApiService = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var repository: NotificationRepositoryImpl

    private val validUuid1 = "5a4637c8-9ddb-40b8-af7c-b15073f8090b"
    private val validUuid2 = "c3b88937-291d-4076-a05e-f007bbf90f38"
    private val legacyId = "proj_5a4637c8-9ddb-40b8-af7c-b15073f8090b_20719"

    @Before
    fun setUp() {
        repository = NotificationRepositoryImpl(
            notificationDao = notificationDao,
            notificationsApiService = notificationsApiService,
            logger = logger
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given canonical UUID when markAsRead then updates DAO optimistically, calls remote API and sets syncStatus to SYNCED`() = runTest {
        coEvery { notificationsApiService.markAsRead(validUuid1) } returns Response.success(
            NotificationReadResponse(success = true, data = NotificationReadData(id = validUuid1, isRead = true))
        )

        repository.markAsRead(validUuid1)

        coVerify(exactly = 1) { notificationDao.markAsRead(validUuid1, any()) }
        coVerify(exactly = 1) { notificationsApiService.markAsRead(validUuid1) }
        coVerify(exactly = 1) { notificationDao.updateSyncStatus(listOf(validUuid1), "SYNCED") }
    }

    @Test
    fun `given canonical UUID when markAsRead API fails then does not update syncStatus to SYNCED`() = runTest {
        coEvery { notificationsApiService.markAsRead(validUuid1) } returns Response.error(
            500,
            "Internal Server Error".toResponseBody("application/json".toMediaTypeOrNull())
        )

        repository.markAsRead(validUuid1)

        coVerify(exactly = 1) { notificationDao.markAsRead(validUuid1, any()) }
        coVerify(exactly = 1) { notificationsApiService.markAsRead(validUuid1) }
        coVerify(exactly = 0) { notificationDao.updateSyncStatus(any(), any()) }
    }

    @Test
    fun `given legacy non-UUID ID when markAsRead then updates DAO optimistically and skips remote API`() = runTest {
        repository.markAsRead(legacyId)

        coVerify(exactly = 1) { notificationDao.markAsRead(legacyId, any()) }
        coVerify(exactly = 0) { notificationsApiService.markAsRead(any()) }
    }

    @Test
    fun `given markAllAsRead then updates DAO optimistically and calls API`() = runTest {
        repository.markAllAsRead()

        coVerify(exactly = 1) { notificationDao.markAllAsRead(any()) }
        coVerify(exactly = 1) { notificationsApiService.markAllAsRead() }
    }

    @Test
    fun `given canonical UUID when delete then removes from DAO optimistically and calls remote API`() = runTest {
        repository.delete(validUuid2)

        coVerify(exactly = 1) { notificationDao.deleteById(validUuid2) }
        coVerify(exactly = 1) { notificationsApiService.deleteNotification(validUuid2) }
    }

    @Test
    fun `given legacy non-UUID ID when delete then removes from DAO optimistically and skips remote API`() = runTest {
        repository.delete(legacyId)

        coVerify(exactly = 1) { notificationDao.deleteById(legacyId) }
        coVerify(exactly = 0) { notificationsApiService.deleteNotification(any()) }
    }

    @Test
    fun `given notification with semantic key when already exists in Room then updates existing preserving UUID`() = runTest {
        val existingEntity = NotificationEntity(
            id = validUuid1,
            topic = "PROJECTION",
            title = "Alerta previa",
            body = "Cuerpo previo",
            dataJson = "{\"projection_key\":\"proj_contract_1_20719\"}",
            createdAt = 1000L,
            timestampMillis = 1000L
        )
        coEvery { notificationDao.findActiveBySemanticKey("proj_contract_1_20719") } returns existingEntity

        val incomingNotif = Notification(
            id = validUuid2, // Different newly generated UUID
            topic = NotificationTopic.PROJECTION,
            title = "Alerta actualizada",
            body = "Cuerpo actualizado",
            priority = NotificationPriority.CRITICAL,
            data = mapOf("projection_key" to "proj_contract_1_20719")
        )

        repository.insertOrUpdate(incomingNotif)

        // Verify it was inserted with existing UUID (validUuid1) instead of new one (validUuid2)
        coVerify(exactly = 1) {
            notificationDao.insertOrUpdate(match { it.id == validUuid1 && it.title == "Alerta actualizada" })
        }
    }

    @Test
    fun `given pending notifications with valid UUID when syncPending then sends batch to API with lowercase topic and ISO dates and marks SYNCED`() = runTest {
        val pending = listOf(
            NotificationEntity(
                id = validUuid1,
                topic = "PROJECTION",
                title = "Alerta",
                body = "Cuerpo",
                dataJson = "{\"projection_key\":\"proj_c1_20719\"}",
                syncStatus = "PENDING"
            )
        )
        coEvery { notificationDao.getPendingSyncNotifications() } returns pending
        coEvery { notificationsApiService.syncNotifications(any()) } returns Response.success(
            NotificationSyncResponse(success = true, data = NotificationSyncData(syncedCount = 1))
        )

        val result = repository.syncPending()

        assertTrue(result)
        coVerify(exactly = 1) { notificationDao.updateSyncStatus(listOf(validUuid1), "SYNCED") }
        coVerify(exactly = 1) {
            notificationsApiService.syncNotifications(match { req ->
                req.notifications.size == 1 &&
                req.notifications[0].id == validUuid1 &&
                req.notifications[0].topic == "projection" &&
                req.notifications[0].createdAt != null &&
                req.notifications[0].createdAt!!.endsWith("Z") &&
                req.notifications[0].data?.get("projection_key") == "proj_c1_20719"
            })
        }
    }

    @Test
    fun `given notification when insertOrUpdate then writes to dao and triggers opportunistic sync`() = runTest {
        val notif = Notification(
            id = validUuid1,
            topic = NotificationTopic.PROJECTION,
            title = "Alerta",
            body = "Cuerpo",
            priority = NotificationPriority.CRITICAL,
            syncStatus = "PENDING"
        )
        coEvery { notificationDao.getPendingSyncNotifications() } returns listOf(notif.toEntity())
        coEvery { notificationsApiService.syncNotifications(any()) } returns Response.success(
            NotificationSyncResponse(success = true, data = NotificationSyncData(syncedCount = 1))
        )

        repository.insertOrUpdate(notif)

        coVerify(atLeast = 1) { notificationDao.insertOrUpdate(match { it.id == validUuid1 }) }
    }

    @Test
    fun `given remote API returns 403 then fetchRemoteNotifications returns PremiumRequiredException`() = runTest {
        val errorBody = "{\"error\": \"PREMIUM_REQUIRED\"}".toResponseBody("application/json".toMediaTypeOrNull())
        coEvery { notificationsApiService.getNotifications(limit = 50) } returns Response.error(403, errorBody)

        val result = repository.fetchRemoteNotifications()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PremiumRequiredException)
    }

    @Test
    fun `given remote API returns items then fetchRemoteNotifications inserts into DAO with origin REMOTE and SYNCED`() = runTest {
        val remoteDto = NotificationRemoteDto(
            id = validUuid1,
            topic = "SUBSCRIPTION",
            title = "Suscripción renovada",
            body = "Plan Pro activo",
            origin = "REMOTE",
            isRead = false
        )
        val response = NotificationsListResponse(
            success = true,
            data = NotificationListData(items = listOf(remoteDto), total = 1, unreadCount = 1)
        )
        coEvery { notificationsApiService.getNotifications(limit = 50) } returns Response.success(response)

        val result = repository.fetchRemoteNotifications()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        coVerify(exactly = 1) {
            notificationDao.insertOrUpdateAll(match { list ->
                list.size == 1 && list[0].id == validUuid1 && list[0].origin == "REMOTE" && list[0].syncStatus == "SYNCED"
            })
        }
    }
}
