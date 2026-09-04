package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
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

class DeleteContractUseCaseTest {

    private val repository: RentingRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: DeleteContractUseCase

    @Before
    fun setUp() {
        useCase = DeleteContractUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given contract id when invoke then deletes contract and emits Progress then Success`() = runTest {
        val contractId = "contract-to-delete"
        every { repository.deleteContract(contractId) } returns flowOf(Unit)

        val emissions = useCase(DeleteContractUseCase.Input(contractId)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is DeleteContractUseCase.Output.Progress)
        assertTrue(emissions[1] is DeleteContractUseCase.Output.Success)

        coVerify(exactly = 1) { repository.deleteContract(contractId) }
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        val contractId = "contract-to-delete"
        every { repository.deleteContract(contractId) } returns flow { throw RuntimeException("Delete failed") }

        val emissions = useCase(DeleteContractUseCase.Input(contractId)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is DeleteContractUseCase.Output.Progress)
        assertTrue(emissions[1] is DeleteContractUseCase.Output.Failure)
    }
}
