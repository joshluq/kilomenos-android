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
import java.util.Calendar

class PredictBestRefuelDayUseCaseTest {

    private val expenseRepository: FuelExpenseRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: PredictBestRefuelDayUseCase

    @Before
    fun setUp() {
        useCase = PredictBestRefuelDayUseCaseImpl(expenseRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given fewer than 3 records when invoke then returns null prediction`() = runTest {
        every { expenseRepository.getExpensesByStation("st-1") } returns flowOf(emptyList())

        val emissions = useCase(PredictBestRefuelDayUseCase.Input("st-1", FuelType.GASOLINE_95)).toList()

        assertEquals(1, emissions.size)
        val success = emissions[0] as PredictBestRefuelDayUseCase.Output.Success
        assertNull(success.bestDayOfWeek)
        assertNull(success.savingPerUnit)
    }

    @Test
    fun `given multiple records across different days when invoke then identifies cheapest day and potential saving`() = runTest {
        // Build timestamps for Tuesday (cheaper: 1.40) and Friday (expensive: 1.60)
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
        val tuesdayTs1 = cal.timeInMillis
        cal.add(Calendar.WEEK_OF_YEAR, -1)
        val tuesdayTs2 = cal.timeInMillis

        cal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
        val fridayTs1 = cal.timeInMillis

        val exp1 = FuelExpense(id = "1", vehicleId = "v1", stationId = "st-1", timestamp = tuesdayTs1, fuelType = FuelType.GASOLINE_95, unitPrice = 1.40, volumeQuantity = 40.0, totalCost = 56.0)
        val exp2 = FuelExpense(id = "2", vehicleId = "v1", stationId = "st-1", timestamp = tuesdayTs2, fuelType = FuelType.GASOLINE_95, unitPrice = 1.42, volumeQuantity = 40.0, totalCost = 56.8)
        val exp3 = FuelExpense(id = "3", vehicleId = "v1", stationId = "st-1", timestamp = fridayTs1, fuelType = FuelType.GASOLINE_95, unitPrice = 1.60, volumeQuantity = 40.0, totalCost = 64.0)

        every { expenseRepository.getExpensesByStation("st-1") } returns flowOf(listOf(exp1, exp2, exp3))

        val emissions = useCase(PredictBestRefuelDayUseCase.Input("st-1", FuelType.GASOLINE_95)).toList()

        assertEquals(1, emissions.size)
        val success = emissions[0] as PredictBestRefuelDayUseCase.Output.Success

        assertEquals(Calendar.TUESDAY, success.bestDayOfWeek)
        assertNotNull(success.bestDayAveragePrice)
        assertEquals(1.41, success.bestDayAveragePrice!!, 0.005)
        assertNotNull(success.savingPerUnit)
    }
}
