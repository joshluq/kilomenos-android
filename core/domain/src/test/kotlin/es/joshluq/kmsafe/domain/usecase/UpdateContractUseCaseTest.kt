package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.RentingContract
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

class UpdateContractUseCaseTest {

    private val repository: RentingRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: UpdateContractUseCase

    @Before
    fun setUp() {
        useCase = UpdateContractUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createContract() = RentingContract(
        id = "c1",
        userId = "u1",
        vehicleName = "BMW 320",
        startDate = 1000L,
        durationMonths = 36,
        totalKms = 45000.0,
        startOdometer = 0.0,
        currentOdometer = 1200.0
    )

    @Test
    fun `given contract when invoke then updates contract and emits Progress then Success`() = runTest {
        val contract = createContract()
        every { repository.updateContract(contract) } returns flowOf(Unit)

        val emissions = useCase(UpdateContractUseCase.Input(contract)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is UpdateContractUseCase.Output.Progress)
        assertTrue(emissions[1] is UpdateContractUseCase.Output.Success)

        coVerify(exactly = 1) { repository.updateContract(contract) }
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        val contract = createContract()
        every { repository.updateContract(contract) } returns flow { throw RuntimeException("Update failed") }

        val emissions = useCase(UpdateContractUseCase.Input(contract)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is UpdateContractUseCase.Output.Progress)
        assertTrue(emissions[1] is UpdateContractUseCase.Output.Failure)
    }
}
