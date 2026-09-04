package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.repository.HistoryRepository
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

class SyncHistoryUseCaseTest {

    private val repository: HistoryRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: SyncHistoryUseCase

    @Before
    fun setUp() {
        useCase = SyncHistoryUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given contractId when invoke then syncs history and emits Progress then Success`() = runTest {
        val contractId = "c-1"
        every { repository.syncHistory(contractId) } returns flowOf(Unit)

        val emissions = useCase(SyncHistoryUseCase.Input(contractId)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SyncHistoryUseCase.Output.Progress)
        assertTrue(emissions[1] is SyncHistoryUseCase.Output.Success)

        coVerify(exactly = 1) { repository.syncHistory(contractId) }
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        val contractId = "c-1"
        every { repository.syncHistory(contractId) } returns flow { throw RuntimeException("Sync failure") }

        val emissions = useCase(SyncHistoryUseCase.Input(contractId)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SyncHistoryUseCase.Output.Progress)
        val failure = emissions[1] as SyncHistoryUseCase.Output.Failure
        assertEquals("Sync failure", failure.message)
    }
}
