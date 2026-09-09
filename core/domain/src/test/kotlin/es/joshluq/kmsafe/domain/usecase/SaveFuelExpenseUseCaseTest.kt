package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
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

class SaveFuelExpenseUseCaseTest {

    private val expenseRepository: FuelExpenseRepository = mockk()
    private val historyRepository: HistoryRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: SaveFuelExpenseUseCase

    @Before
    fun setUp() {
        useCase = SaveFuelExpenseUseCaseImpl(expenseRepository, historyRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given invalid negative values when invoke then emits Progress and InvalidInput`() = runTest {
        val input = SaveFuelExpenseUseCase.Input(
            vehicleId = "veh-1",
            fuelType = FuelType.GASOLINE_95,
            unitPrice = 1.5,
            volumeQuantity = -10.0,
            totalCost = -15.0
        )

        val emissions = useCase(input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SaveFuelExpenseUseCase.Output.Progress)
        val invalid = emissions[1] as SaveFuelExpenseUseCase.Output.InvalidInput
        assertEquals(KmError.InvalidFuelExpenseValues, invalid.error)
    }

    @Test
    fun `given invalid negative odometer when invoke then emits Progress and InvalidInput`() = runTest {
        val input = SaveFuelExpenseUseCase.Input(
            vehicleId = "veh-1",
            fuelType = FuelType.GASOLINE_95,
            unitPrice = 1.5,
            volumeQuantity = 10.0,
            totalCost = 15.0,
            odometerAtExpense = -10.0
        )

        val emissions = useCase(input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SaveFuelExpenseUseCase.Output.Progress)
        val invalid = emissions[1] as SaveFuelExpenseUseCase.Output.InvalidInput
        assertEquals(KmError.InvalidFuelExpenseValues, invalid.error)
    }

    @Test
    fun `given valid expense with isFullTank false when invoke then skips consumption calculation and emits Success`() = runTest {
        val input = SaveFuelExpenseUseCase.Input(
            vehicleId = "veh-1",
            fuelType = FuelType.GASOLINE_95,
            unitPrice = 1.5,
            volumeQuantity = 20.0,
            totalCost = 30.0,
            isFullTank = false
        )

        every { expenseRepository.saveExpense(any()) } returns flowOf("expense-123")

        val emissions = useCase(input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SaveFuelExpenseUseCase.Output.Progress)
        val success = emissions[1] as SaveFuelExpenseUseCase.Output.Success
        assertEquals("expense-123", success.expenseId)

        coVerify(exactly = 1) { expenseRepository.saveExpense(match { it.consumptionPer100km == null }) }
    }

    @Test
    fun `given valid expense with isFullTank true and odometer history when invoke then computes consumption and emits Success`() = runTest {
        val lastRefuelTime = 1000L
        val input = SaveFuelExpenseUseCase.Input(
            vehicleId = "veh-1",
            fuelType = FuelType.GASOLINE_95,
            unitPrice = 1.6,
            volumeQuantity = 40.0,
            totalCost = 64.0,
            isFullTank = true,
            lastRefuelTimestamp = lastRefuelTime
        )

        val oldRecord = OdometerRecord(
            id = "r0",
            contractId = "veh-1",
            timestamp = 500L,
            odometerValue = 100.0,
            isInitialRecord = false
        )
        val newRecord = OdometerRecord(
            id = "r1",
            contractId = "veh-1",
            timestamp = 2000L,
            odometerValue = 500.0, // 500 km driven since last full refuel
            isInitialRecord = false
        )

        every { historyRepository.getHistory("veh-1") } returns flowOf(listOf(oldRecord, newRecord))
        every { expenseRepository.saveExpense(any()) } returns flowOf("expense-456")

        val emissions = useCase(input).toList()

        assertEquals(2, emissions.size)
        val success = emissions[1] as SaveFuelExpenseUseCase.Output.Success
        assertEquals("expense-456", success.expenseId)

        // 40 liters for 500 km = 8.0 L/100km
        coVerify(exactly = 1) {
            expenseRepository.saveExpense(match {
                it.kmSinceLastRefuel == 500.0 && it.consumptionPer100km == 8.0
            })
        }
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        val input = SaveFuelExpenseUseCase.Input(
            vehicleId = "veh-1",
            fuelType = FuelType.GASOLINE_95,
            unitPrice = 1.5,
            volumeQuantity = 20.0,
            totalCost = 30.0,
            isFullTank = false
        )

        every { expenseRepository.saveExpense(any()) } returns flow { throw RuntimeException("Error saving") }

        val emissions = useCase(input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SaveFuelExpenseUseCase.Output.Progress)
        val failure = emissions[1] as SaveFuelExpenseUseCase.Output.Failure
        assertEquals(KmError.UnknownError, failure.error)
    }
}
