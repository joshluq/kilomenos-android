package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
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

class GetElectrificationSavingsUseCaseTest {

    private val expenseRepository: FuelExpenseRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: GetElectrificationSavingsUseCase

    @Before
    fun setUp() {
        useCase = GetElectrificationSavingsUseCaseImpl(expenseRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createExpense(fuelType: FuelType, volumeKwh: Double, cost: Double) = FuelExpense(
        id = "e1",
        vehicleId = "v1",
        timestamp = 1000L,
        fuelType = fuelType,
        unitPrice = cost / volumeKwh,
        volumeQuantity = volumeKwh,
        totalCost = cost
    )

    @Test
    fun `given no electric expenses when invoke then emits Progress and Empty`() = runTest {
        val gasolineExpense = createExpense(FuelType.GASOLINE_95, 40.0, 60.0)
        every { expenseRepository.getExpensesByVehicle("v1") } returns flowOf(listOf(gasolineExpense))

        val emissions = useCase(GetElectrificationSavingsUseCase.Input("v1")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetElectrificationSavingsUseCase.Output.Progress)
        assertTrue(emissions[1] is GetElectrificationSavingsUseCase.Output.Empty)
    }

    @Test
    fun `given electric expenses when invoke then emits Progress and Success with computed savings`() = runTest {
        // 170 kWh consumed at 34 €
        val electricExpense = createExpense(FuelType.ELECTRIC_KWH, 170.0, 34.0)
        every { expenseRepository.getExpensesByVehicle("v1") } returns flowOf(listOf(electricExpense))

        val emissions = useCase(GetElectrificationSavingsUseCase.Input("v1")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetElectrificationSavingsUseCase.Output.Progress)
        val success = emissions[1] as GetElectrificationSavingsUseCase.Output.Success

        assertEquals(170.0, success.totalKwh, 0.001)
        assertEquals(34.0, success.totalElectricCost, 0.001)
        // 170 kWh / 17 kWh/100km * 100 = 1000 km driven
        assertEquals(1000.0, success.equivalentKmDriven, 0.001)
        // 1000 km / 100 * 6.5 L = 65 L. 65 L * 1.85 €/L = 120.25 €
        assertEquals(120.25, success.theoreticalGasolineCost, 0.001)
        // 120.25 - 34.0 = 86.25 € saved
        assertEquals(86.25, success.totalSavedEuros, 0.001)
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        every { expenseRepository.getExpensesByVehicle("v1") } returns flow { throw RuntimeException("Error") }

        val emissions = useCase(GetElectrificationSavingsUseCase.Input("v1")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetElectrificationSavingsUseCase.Output.Progress)
        assertTrue(emissions[1] is GetElectrificationSavingsUseCase.Output.Failure)
    }
}
