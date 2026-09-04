package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CalculateCostPerHundredKmUseCaseTest {

    private val expenseRepository: FuelExpenseRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: CalculateCostPerHundredKmUseCase

    @Before
    fun setUp() {
        useCase = CalculateCostPerHundredKmUseCaseImpl(expenseRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given no full tank expenses when invoke then returns null metrics`() = runTest {
        every { expenseRepository.getExpensesByVehicle("veh-1") } returns flowOf(emptyList())

        val emissions = useCase(CalculateCostPerHundredKmUseCase.Input("veh-1")).toList()

        assertEquals(1, emissions.size)
        val success = emissions[0] as CalculateCostPerHundredKmUseCase.Output.Success
        assertNull(success.costPer100km)
        assertNull(success.lastCycleConsumption)
        assertNull(success.averageConsumption)
        assertEquals(0.0, success.totalFullTankKms, 0.001)
    }

    @Test
    fun `given full tank expenses when invoke then computes cost per 100km and consumption correctly`() = runTest {
        val expense1 = FuelExpense(
            id = "exp-1",
            vehicleId = "veh-1",
            timestamp = 1000L,
            fuelType = FuelType.GASOLINE_95,
            unitPrice = 1.50,
            volumeQuantity = 40.0,
            totalCost = 60.0,
            isFullTank = true,
            kmSinceLastRefuel = 500.0,
            consumptionPer100km = 8.0 // 40L / 500km * 100 = 8.0
        )
        val expense2 = FuelExpense(
            id = "exp-2",
            vehicleId = "veh-1",
            timestamp = 2000L,
            fuelType = FuelType.GASOLINE_95,
            unitPrice = 1.60,
            volumeQuantity = 45.0,
            totalCost = 72.0,
            isFullTank = true,
            kmSinceLastRefuel = 600.0,
            consumptionPer100km = 7.5 // 45L / 600km * 100 = 7.5
        )

        every { expenseRepository.getExpensesByVehicle("veh-1") } returns flowOf(listOf(expense1, expense2))

        val emissions = useCase(CalculateCostPerHundredKmUseCase.Input("veh-1")).toList()

        assertEquals(1, emissions.size)
        val success = emissions[0] as CalculateCostPerHundredKmUseCase.Output.Success

        // Total cost = 60 + 72 = 132. Total kms = 500 + 600 = 1100.
        // Cost per 100km = (132 / 1100) * 100 = 12.0
        assertNotNull(success.costPer100km)
        assertEquals(12.0, success.costPer100km!!, 0.001)
        assertEquals(1100.0, success.totalFullTankKms, 0.001)
        assertEquals(132.0, success.totalFullTankCost, 0.001)

        // Last cycle consumption = 7.5 (from expense2 at 2000L)
        assertEquals(7.5, success.lastCycleConsumption!!, 0.001)
        // Average consumption = (8.0 + 7.5) / 2 = 7.75
        assertEquals(7.75, success.averageConsumption!!, 0.001)
        // Delta = 7.5 - 7.75 = -0.25 (better than average)
        assertEquals(-0.25, success.consumptionDeltaVsAverage!!, 0.001)
    }
}
