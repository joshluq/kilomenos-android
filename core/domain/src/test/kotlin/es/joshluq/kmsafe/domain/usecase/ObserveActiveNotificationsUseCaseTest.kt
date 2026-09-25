package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationPriority
import es.joshluq.kmsafe.domain.model.NotificationStatus
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.domain.repository.NotificationRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ObserveActiveNotificationsUseCaseTest {

    private val repository: NotificationRepository = mockk()
    private lateinit var useCase: ObserveActiveNotificationsUseCase

    @Before
    fun setUp() {
        useCase = ObserveActiveNotificationsUseCaseImpl(repository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given notifications when observed then returns list from repository`() = runTest {
        val sampleList = listOf(
            Notification(
                id = "1",
                topic = NotificationTopic.PROJECTION,
                title = "Alerta de exceso",
                body = "Desviación del 10%",
                priority = NotificationPriority.CRITICAL,
                status = NotificationStatus.UNREAD
            )
        )
        every { repository.observeNotifications(null) } returns flowOf(sampleList)

        val output = useCase(ObserveActiveNotificationsUseCase.Input()).first()

        val success = output as ObserveActiveNotificationsUseCase.Output.Success
        assertEquals(1, success.notifications.size)
        assertEquals("Alerta de exceso", success.notifications[0].title)
    }

    @Test
    fun `given topic filter when observed then calls repository with topic`() = runTest {
        val sampleList = listOf(
            Notification(
                id = "2",
                topic = NotificationTopic.SUBSCRIPTION,
                title = "Vencimiento próximo",
                body = "Vence en 3 días",
                priority = NotificationPriority.WARNING,
                status = NotificationStatus.UNREAD
            )
        )
        every { repository.observeNotifications(NotificationTopic.SUBSCRIPTION) } returns flowOf(sampleList)

        val output = useCase(ObserveActiveNotificationsUseCase.Input(NotificationTopic.SUBSCRIPTION)).first()

        val success = output as ObserveActiveNotificationsUseCase.Output.Success
        assertEquals(1, success.notifications.size)
        assertEquals(NotificationTopic.SUBSCRIPTION, success.notifications[0].topic)
    }
}
