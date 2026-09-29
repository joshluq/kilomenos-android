package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import es.joshluq.kmsafe.domain.repository.NotificationRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncEntitlementsFromPushUseCaseTest {

    private val entitlementsRepository: EntitlementsRepository = mockk(relaxed = true)
    private val notificationRepository: NotificationRepository = mockk(relaxed = true)
    private lateinit var useCase: SyncEntitlementsFromPushUseCase

    @Before
    fun setUp() {
        useCase = SyncEntitlementsFromPushUseCaseImpl(entitlementsRepository, notificationRepository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given downgrade push payload when invoke then clears cache and posts notification`() = runTest {
        val payload = mapOf(
            "type" to "ENTITLEMENTS_SYNC",
            "subscription_level" to "FREE",
            "is_downgrade" to "true"
        )

        val notificationSlot = slot<Notification>()
        coEvery { notificationRepository.insertOrUpdate(capture(notificationSlot)) } returns Unit

        val result = useCase(SyncEntitlementsFromPushUseCase.Input(payload))

        assertTrue(result.isSuccess)
        val output = result.getOrNull() as SyncEntitlementsFromPushUseCase.Output.Success
        assertTrue(output.isDowngraded)

        verify(exactly = 1) { entitlementsRepository.clearCache() }
        coVerify(exactly = 1) { entitlementsRepository.downgradeToFree() }
        coVerify(exactly = 1) { notificationRepository.insertOrUpdate(any()) }
        assertEquals(NotificationTopic.SUBSCRIPTION, notificationSlot.captured.topic)
        assertEquals("Tu plan ha cambiado a Free", notificationSlot.captured.title)
        assertEquals("kmsafe://app/notifications", notificationSlot.captured.deepLinkUri)
        assertEquals("SYNCED", notificationSlot.captured.syncStatus)
        assertEquals("REMOTE", notificationSlot.captured.origin)
    }

    @Test
    fun `given upgrade push payload when invoke then clears cache without downgrade alert`() = runTest {
        val payload = mapOf(
            "type" to "ENTITLEMENTS_SYNC",
            "subscription_level" to "PREMIUM",
            "is_downgrade" to "false"
        )

        val result = useCase(SyncEntitlementsFromPushUseCase.Input(payload))

        assertTrue(result.isSuccess)
        val output = result.getOrNull() as SyncEntitlementsFromPushUseCase.Output.Success
        assertEquals(false, output.isDowngraded)

        verify(exactly = 1) { entitlementsRepository.clearCache() }
        coVerify(exactly = 0) { entitlementsRepository.downgradeToFree() }
        coVerify(exactly = 0) { notificationRepository.insertOrUpdate(any()) }
    }

    @Test
    fun `given RTDN FCM HTTP v1 downgrade payload when invoke then clears cache and posts notification`() = runTest {
        val payload = mapOf(
            "event_type" to "SUBSCRIPTION_DOWNGRADED",
            "subscription_level" to "FREE",
            "previous_level" to "PREMIUM",
            "action_code" to "REFRESH_ENTITLEMENTS",
            "title" to "Suscripción Finalizada",
            "message" to "Tu período Premium ha finalizado.",
            "deep_link" to "kmsafe://app/notifications"
        )

        val notificationSlot = slot<Notification>()
        coEvery { notificationRepository.insertOrUpdate(capture(notificationSlot)) } returns Unit

        val result = useCase(SyncEntitlementsFromPushUseCase.Input(payload))

        assertTrue(result.isSuccess)
        val output = result.getOrNull() as SyncEntitlementsFromPushUseCase.Output.Success
        assertTrue(output.isDowngraded)

        verify(exactly = 1) { entitlementsRepository.clearCache() }
        coVerify(exactly = 1) { entitlementsRepository.downgradeToFree() }
        coVerify(exactly = 1) { notificationRepository.insertOrUpdate(any()) }
        assertEquals(NotificationTopic.SUBSCRIPTION, notificationSlot.captured.topic)
        assertEquals("Suscripción Finalizada", notificationSlot.captured.title)
        assertEquals("kmsafe://app/notifications", notificationSlot.captured.deepLinkUri)
    }

    @Test
    fun `given FCM HTTP v1 payload with canonical notification_id then persists notification with exact id and SYNCED status`() = runTest {
        val expectedUuid = "197b6717-7a2c-482c-8cbc-b1332bbbb457"
        val payload = mapOf(
            "notification_id" to expectedUuid,
            "event_type" to "SUBSCRIPTION_DOWNGRADED",
            "subscription_level" to "FREE",
            "action_code" to "REFRESH_ENTITLEMENTS",
            "title" to "Tu plan ha cambiado a Free",
            "body" to "Tu período Premium ha finalizado.",
            "deep_link" to "kmsafe://app/notifications"
        )

        val notificationSlot = slot<Notification>()
        coEvery { notificationRepository.insertOrUpdate(capture(notificationSlot)) } returns Unit

        val result = useCase(SyncEntitlementsFromPushUseCase.Input(payload))

        assertTrue(result.isSuccess)
        val output = result.getOrNull() as SyncEntitlementsFromPushUseCase.Output.Success
        assertTrue(output.isDowngraded)
        assertEquals(expectedUuid, output.notification?.id)

        coVerify(exactly = 1) { notificationRepository.insertOrUpdate(any()) }
        assertEquals(expectedUuid, notificationSlot.captured.id)
        assertEquals("SYNCED", notificationSlot.captured.syncStatus)
        assertEquals("REMOTE", notificationSlot.captured.origin)
    }
}
