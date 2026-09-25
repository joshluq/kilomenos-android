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
        coVerify(exactly = 1) { notificationRepository.insertOrUpdate(any()) }
        assertEquals(NotificationTopic.SUBSCRIPTION, notificationSlot.captured.topic)
        assertEquals("Tu suscripción ha cambiado a Plan Gratuito", notificationSlot.captured.title)
        assertEquals("kmsafe://feature/premium", notificationSlot.captured.deepLinkUri)
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
        coVerify(exactly = 0) { notificationRepository.insertOrUpdate(any()) }
    }
}
