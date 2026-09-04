package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.repository.RentingRepository
import io.mockk.clearAllMocks
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

class GetRentingContractUseCaseTest {

    private val repository: RentingRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: GetRentingContractUseCase

    @Before
    fun setUp() {
        useCase = GetRentingContractUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createContract() = RentingContract(
        id = "c1",
        userId = "u1",
        vehicleName = "Audi A4",
        startDate = 1000L,
        durationMonths = 24,
        totalKms = 30000.0,
        startOdometer = 0.0,
        currentOdometer = 500.0
    )

    @Test
    fun `given existing contract when invoke then emits Progress and Success`() = runTest {
        val contract = createContract()
        every { repository.getContract() } returns flowOf(contract)

        val emissions = useCase(GetRentingContractUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetRentingContractUseCase.Output.Progress)
        val success = emissions[1] as GetRentingContractUseCase.Output.Success
        assertEquals(contract, success.contract)
    }

    @Test
    fun `given null contract when invoke then emits Progress and Failure`() = runTest {
        every { repository.getContract() } returns flowOf(null)

        val emissions = useCase(GetRentingContractUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetRentingContractUseCase.Output.Progress)
        assertTrue(emissions[1] is GetRentingContractUseCase.Output.Failure)
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        every { repository.getContract() } returns flow { throw RuntimeException("Disk read failure") }

        val emissions = useCase(GetRentingContractUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetRentingContractUseCase.Output.Progress)
        assertTrue(emissions[1] is GetRentingContractUseCase.Output.Failure)
    }
}
