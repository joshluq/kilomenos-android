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

class GetVehicleByIdUseCaseTest {

    private val repository: RentingRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: GetVehicleByIdUseCase

    @Before
    fun setUp() {
        useCase = GetVehicleByIdUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createContract(id: String) = RentingContract(
        id = id,
        userId = "u1",
        vehicleName = "Tesla Model 3",
        startDate = 1000L,
        durationMonths = 48,
        totalKms = 60000.0,
        startOdometer = 0.0,
        currentOdometer = 2500.0
    )

    @Test
    fun `given existing vehicle id when invoke then emits Progress and Success`() = runTest {
        val contract = createContract("veh-1")
        every { repository.getContractById("veh-1") } returns flowOf(contract)

        val emissions = useCase(GetVehicleByIdUseCase.Input("veh-1")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetVehicleByIdUseCase.Output.Progress)
        val success = emissions[1] as GetVehicleByIdUseCase.Output.Success
        assertEquals(contract, success.contract)

        coVerify(exactly = 1) { repository.getContractById("veh-1") }
    }

    @Test
    fun `given non-existing vehicle id when invoke then emits Progress and Failure`() = runTest {
        every { repository.getContractById("non-existing") } returns flowOf(null)

        val emissions = useCase(GetVehicleByIdUseCase.Input("non-existing")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetVehicleByIdUseCase.Output.Progress)
        assertTrue(emissions[1] is GetVehicleByIdUseCase.Output.Failure)
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        every { repository.getContractById("veh-1") } returns flow { throw RuntimeException("Database error") }

        val emissions = useCase(GetVehicleByIdUseCase.Input("veh-1")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetVehicleByIdUseCase.Output.Progress)
        assertTrue(emissions[1] is GetVehicleByIdUseCase.Output.Failure)
    }
}
