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

class StopTrackingUseCaseTest {

    private val repository: TrackingRepository = mockk(relaxed = true)

    private lateinit var useCase: StopTrackingUseCase

    @Before
    fun setUp() {
        useCase = StopTrackingUseCaseImpl(repository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `when invoke then calls stopTracking and emits Success`() = runTest {
        val emissions = useCase(StopTrackingUseCase.Input).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0] is StopTrackingUseCase.Output.Success)

        coVerify(exactly = 1) { repository.stopTracking() }
    }
}
