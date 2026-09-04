package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
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

class SelectContractUseCaseTest {

    private val rentingRepository: RentingRepository = mockk()
    private val historyRepository: HistoryRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: SelectContractUseCase

    @Before
    fun setUp() {
        useCase = SelectContractUseCaseImpl(rentingRepository, historyRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given valid id when invoke then selects contract, syncs history and emits Progress then Success`() = runTest {
        val contractId = "contract-123"
        every { rentingRepository.selectContract(contractId) } returns flowOf(Unit)
        every { historyRepository.syncHistory(contractId) } returns flowOf(Unit)

        val emissions = useCase(SelectContractUseCase.Input(contractId)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SelectContractUseCase.Output.Progress)
        assertTrue(emissions[1] is SelectContractUseCase.Output.Success)

        coVerify(exactly = 1) { rentingRepository.selectContract(contractId) }
        coVerify(exactly = 1) { historyRepository.syncHistory(contractId) }
    }

    @Test
    fun `given rentingRepository error when invoke then catches and emits Failure`() = runTest {
        val contractId = "contract-123"
        every { rentingRepository.selectContract(contractId) } returns flow { throw RuntimeException("Select error") }

        val emissions = useCase(SelectContractUseCase.Input(contractId)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SelectContractUseCase.Output.Progress)
        assertTrue(emissions[1] is SelectContractUseCase.Output.Failure)
    }

    @Test
    fun `given historyRepository sync error when invoke then catches and emits Failure`() = runTest {
        val contractId = "contract-123"
        every { rentingRepository.selectContract(contractId) } returns flowOf(Unit)
        every { historyRepository.syncHistory(contractId) } returns flow { throw RuntimeException("Sync error") }

        val emissions = useCase(SelectContractUseCase.Input(contractId)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SelectContractUseCase.Output.Progress)
        assertTrue(emissions[1] is SelectContractUseCase.Output.Failure)
    }
}
