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

class StartAutoTrackingUseCaseTest {

    private val repository: TrackingRepository = mockk(relaxed = true)

    private lateinit var useCase: StartAutoTrackingUseCase

    @Before
    fun setUp() {
        useCase = StartAutoTrackingUseCaseImpl(repository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `when invoke then calls startAutoTracking and emits Success`() = runTest {
        val emissions = useCase(StartAutoTrackingUseCase.Input).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0] is StartAutoTrackingUseCase.Output.Success)

        coVerify(exactly = 1) { repository.startAutoTracking() }
    }
}
