package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.repository.TrackingRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ObserveTrackingStateUseCaseTest {

    private val repository: TrackingRepository = mockk()

    private lateinit var useCase: ObserveTrackingStateUseCase

    @Before
    fun setUp() {
        useCase = ObserveTrackingStateUseCaseImpl(repository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given tracking repository states when invoke then emits combined Success output`() = runTest {
        every { repository.isTracking } returns flowOf(true)
        every { repository.currentDistanceMeters } returns flowOf(1540.0)
        every { repository.startTime } returns flowOf(123456789L)
        every { repository.currentRoutePolyline } returns flowOf("abc_xyz")
        every { repository.pointCount } returns flowOf(18)

        val emissions = useCase(ObserveTrackingStateUseCase.Input).toList()

        assertEquals(1, emissions.size)
        val success = emissions[0] as ObserveTrackingStateUseCase.Output.Success
        assertTrue(success.isTracking)
        assertEquals(1540.0, success.trackedDistance, 0.001)
        assertEquals(123456789L, success.startTime)
        assertEquals("abc_xyz", success.encodedPolyline)
        assertEquals(18, success.pointCount)
    }
}
