package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.repository.TrackingRepository
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StopAutoTrackingUseCaseTest {

    private val repository: TrackingRepository = mockk(relaxed = true)

    private lateinit var useCase: StopAutoTrackingUseCase

    @Before
    fun setUp() {
        useCase = StopAutoTrackingUseCaseImpl(repository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `when invoke then calls stopAutoTracking and emits Success`() = runTest {
        val emissions = useCase(StopAutoTrackingUseCase.Input).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0] is StopAutoTrackingUseCase.Output.Success)

        coVerify(exactly = 1) { repository.stopAutoTracking() }
    }
}
