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

class GetAllContractsUseCaseTest {

    private val repository: RentingRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: GetAllContractsUseCase

    @Before
    fun setUp() {
        useCase = GetAllContractsUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createContract(id: String, name: String) = RentingContract(
        id = id,
        userId = "u1",
        vehicleName = name,
        startDate = 1000L,
        durationMonths = 12,
        totalKms = 15000.0,
        startOdometer = 0.0,
        currentOdometer = 100.0
    )

    @Test
    fun `given contracts list when invoke then emits Progress and Success with contracts`() = runTest {
        val contracts = listOf(
            createContract("c1", "Car 1"),
            createContract("c2", "Car 2")
        )
        every { repository.getAllContracts() } returns flowOf(contracts)

        val emissions = useCase(GetAllContractsUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetAllContractsUseCase.Output.Progress)
        val success = emissions[1] as GetAllContractsUseCase.Output.Success
        assertEquals(2, success.contracts.size)
        assertEquals(contracts, success.contracts)
    }

    @Test
    fun `given empty contracts list when invoke then emits Progress and Success with empty list`() = runTest {
        every { repository.getAllContracts() } returns flowOf(emptyList())

        val emissions = useCase(GetAllContractsUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetAllContractsUseCase.Output.Progress)
        val success = emissions[1] as GetAllContractsUseCase.Output.Success
        assertTrue(success.contracts.isEmpty())
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        every { repository.getAllContracts() } returns flow { throw RuntimeException("Database error") }

        val emissions = useCase(GetAllContractsUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetAllContractsUseCase.Output.Progress)
        assertTrue(emissions[1] is GetAllContractsUseCase.Output.Failure)
    }
}
