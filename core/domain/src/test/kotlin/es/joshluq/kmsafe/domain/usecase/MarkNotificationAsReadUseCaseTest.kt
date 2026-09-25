package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.repository.NotificationRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class MarkNotificationAsReadUseCaseTest {

    private val repository: NotificationRepository = mockk(relaxed = true)
    private lateinit var useCase: MarkNotificationAsReadUseCase

    @Before
    fun setUp() {
        useCase = MarkNotificationAsReadUseCaseImpl(repository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given notification id when invoke then marks notification as read in repository`() = runTest {
        coEvery { repository.markAsRead("notif-123") } returns Unit

        useCase(MarkNotificationAsReadUseCase.Input("notif-123"))

        coVerify(exactly = 1) { repository.markAsRead("notif-123") }
    }
}
