package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncStationsUseCaseTest {

    private val repository: ServiceStationRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: SyncStationsUseCase

    @Before
    fun setUp() {
        useCase = SyncStationsUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `when invoke then synchronizes stations and emits Progress then Success`() = runTest {
        every { repository.syncStations() } returns flowOf(emptyList())

        val emissions = useCase(SyncStationsUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SyncStationsUseCase.Output.Progress)
        assertTrue(emissions[1] is SyncStationsUseCase.Output.Success)

        coVerify(exactly = 1) { repository.syncStations() }
    }

    @Test
    fun `given error when invoke then catches and emits Failure with message`() = runTest {
        every { repository.syncStations() } returns flow { throw RuntimeException("Network error") }

        val emissions = useCase(SyncStationsUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SyncStationsUseCase.Output.Progress)
        val failure = emissions[1] as SyncStationsUseCase.Output.Failure
        assertEquals("Network error", failure.message)
    }
}
