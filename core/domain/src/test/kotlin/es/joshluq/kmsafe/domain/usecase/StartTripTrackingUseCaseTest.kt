package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.service.TrackingServiceController
import io.mockk.clearAllMocks
import io.mockk.verify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StartTripTrackingUseCaseTest {

    private val controller: TrackingServiceController = mockk(relaxed = true)
    private lateinit var useCase: StartTripTrackingUseCase

    @Before
    fun setUp() {
        useCase = StartTripTrackingUseCaseImpl(controller)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given invoke then delegates to controller startTrackingService`() = runTest {
        val result = useCase(StartTripTrackingUseCase.Input)

        assertEquals(Result.success(StartTripTrackingUseCase.Output), result)
        verify(exactly = 1) { controller.startTrackingService() }
    }
}
