package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationPriority
import es.joshluq.kmsafe.domain.model.NotificationStatus
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.domain.repository.NotificationRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PublishNotificationIfUnreadUseCaseTest {

    private val repository: NotificationRepository = mockk(relaxed = true)
    private val publishNotificationUseCase: PublishNotificationUseCase = mockk(relaxed = true)

    private lateinit var useCase: PublishNotificationIfUnreadUseCaseImpl

    @Before
    fun setUp() {
        useCase = PublishNotificationIfUnreadUseCaseImpl(
            repository = repository,
            publishNotificationUseCase = publishNotificationUseCase
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given notification without semantic key when invoke then delegates and publishes`() = runTest {
        val notif = Notification(
            id = "notif-1",
            topic = NotificationTopic.SYSTEM,
            title = "Aviso general",
            body = "Cuerpo",
            priority = NotificationPriority.INFO,
            status = NotificationStatus.UNREAD,
            data = null
        )

        val result = useCase(PublishNotificationIfUnreadUseCase.Input(notif))

        assertTrue(result.isSuccess)
        assertEquals(PublishNotificationIfUnreadUseCase.Output.Published, result.getOrNull())
        coVerify(exactly = 1) {
            publishNotificationUseCase(PublishNotificationUseCase.Input(notif))
        }
    }

    @Test
    fun `given notification with semantic key not existing in repository when invoke then publishes`() = runTest {
        val notif = Notification(
            id = "notif-2",
            topic = NotificationTopic.PROJECTION,
            title = "Alerta de proyección",
            body = "Cuerpo",
            priority = NotificationPriority.CRITICAL,
            status = NotificationStatus.UNREAD,
            data = mapOf("projection_key" to "proj_c1_20719")
        )
        coEvery { repository.getNotificationBySemanticKey("proj_c1_20719") } returns null

        val result = useCase(PublishNotificationIfUnreadUseCase.Input(notif))

        assertTrue(result.isSuccess)
        assertEquals(PublishNotificationIfUnreadUseCase.Output.Published, result.getOrNull())
        coVerify(exactly = 1) {
            publishNotificationUseCase(PublishNotificationUseCase.Input(notif))
        }
    }

    @Test
    fun `given notification with semantic key existing unread in repository when invoke then publishes`() = runTest {
        val notif = Notification(
            id = "notif-3",
            topic = NotificationTopic.PROJECTION,
            title = "Alerta de proyección",
            body = "Cuerpo",
            priority = NotificationPriority.CRITICAL,
            status = NotificationStatus.UNREAD,
            data = mapOf("projection_key" to "proj_c1_20719")
        )
        val existingUnread = Notification(
            id = "existing-uuid",
            topic = NotificationTopic.PROJECTION,
            title = "Alerta",
            body = "Cuerpo",
            priority = NotificationPriority.CRITICAL,
            isRead = false,
            status = NotificationStatus.UNREAD
        )
        coEvery { repository.getNotificationBySemanticKey("proj_c1_20719") } returns existingUnread

        val result = useCase(PublishNotificationIfUnreadUseCase.Input(notif))

        assertTrue(result.isSuccess)
        assertEquals(PublishNotificationIfUnreadUseCase.Output.Published, result.getOrNull())
        coVerify(exactly = 1) {
            publishNotificationUseCase(PublishNotificationUseCase.Input(notif))
        }
    }

    @Test
    fun `given notification with semantic key already read with isRead true when invoke then skips publication`() = runTest {
        val notif = Notification(
            id = "notif-4",
            topic = NotificationTopic.PROJECTION,
            title = "Alerta de proyección",
            body = "Cuerpo",
            priority = NotificationPriority.CRITICAL,
            status = NotificationStatus.UNREAD,
            data = mapOf("projection_key" to "proj_c1_20719")
        )
        val existingRead = Notification(
            id = "existing-uuid",
            topic = NotificationTopic.PROJECTION,
            title = "Alerta",
            body = "Cuerpo",
            priority = NotificationPriority.CRITICAL,
            isRead = true,
            status = NotificationStatus.READ
        )
        coEvery { repository.getNotificationBySemanticKey("proj_c1_20719") } returns existingRead

        val result = useCase(PublishNotificationIfUnreadUseCase.Input(notif))

        assertTrue(result.isSuccess)
        assertEquals(PublishNotificationIfUnreadUseCase.Output.SkippedAlreadyRead, result.getOrNull())
        coVerify(exactly = 0) {
            publishNotificationUseCase(any())
        }
    }

    @Test
    fun `given notification with deduplication_key already read with status READ when invoke then skips publication`() = runTest {
        val notif = Notification(
            id = "notif-5",
            topic = NotificationTopic.SYSTEM,
            title = "Dispositivo Bluetooth no configurado",
            body = "Configura el Bluetooth",
            priority = NotificationPriority.WARNING,
            status = NotificationStatus.UNREAD,
            data = mapOf("deduplication_key" to "bt_missing_c1")
        )
        val existingRead = Notification(
            id = "existing-uuid-2",
            topic = NotificationTopic.SYSTEM,
            title = "Dispositivo Bluetooth no configurado",
            body = "Cuerpo",
            priority = NotificationPriority.WARNING,
            isRead = false,
            status = NotificationStatus.READ
        )
        coEvery { repository.getNotificationBySemanticKey("bt_missing_c1") } returns existingRead

        val result = useCase(PublishNotificationIfUnreadUseCase.Input(notif))

        assertTrue(result.isSuccess)
        assertEquals(PublishNotificationIfUnreadUseCase.Output.SkippedAlreadyRead, result.getOrNull())
        coVerify(exactly = 0) {
            publishNotificationUseCase(any())
        }
    }
}
