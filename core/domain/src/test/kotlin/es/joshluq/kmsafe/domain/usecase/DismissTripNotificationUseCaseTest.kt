package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.service.TrackingServiceController
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DismissTripNotificationUseCaseTest {

    private val controller: TrackingServiceController = mockk(relaxed = true)
    private lateinit var useCase: DismissTripNotificationUseCase

    @Before
    fun setUp() {
        useCase = DismissTripNotificationUseCaseImpl(controller)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given invoke then delegates to controller dismissTripFinishedNotification`() = runTest {
        val result = useCase(DismissTripNotificationUseCase.Input)

        assertEquals(Result.success(DismissTripNotificationUseCase.Output), result)
        verify(exactly = 1) { controller.dismissTripFinishedNotification() }
    }
}
