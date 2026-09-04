package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import es.joshluq.kmsafe.domain.repository.HistoryRepository
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

class GetExpensesByVehicleUseCaseTest {

    private val expenseRepository: FuelExpenseRepository = mockk()
    private val rentingRepository: RentingRepository = mockk()
    private val historyRepository: HistoryRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: GetExpensesByVehicleUseCase

    @Before
    fun setUp() {
        useCase = GetExpensesByVehicleUseCaseImpl(
            expenseRepository = expenseRepository,
            rentingRepository = rentingRepository,
            historyRepository = historyRepository,
            logger = logger
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createContract(id: String = "c1") = RentingContract(
        id = id,
        userId = "u1",
        vehicleName = "Golf",
        startDate = 1000L,
        durationMonths = 12,
        totalKms = 12000.0,
        startOdometer = 5000.0,
        currentOdometer = 5000.0,
        fuelType = FuelType.GASOLINE_95
    )

    private fun createExpense(
        id: String,
        vehicleId: String,
        totalCost: Double,
        volume: Double,
        timestamp: Long = System.currentTimeMillis()
    ) = FuelExpense(
        id = id,
        vehicleId = vehicleId,
        timestamp = timestamp,
        fuelType = FuelType.GASOLINE_95,
        unitPrice = 1.6,
        volumeQuantity = volume,
        totalCost = totalCost,
        isFullTank = true
    )

    @Test
    fun `given no contract when invoke then emits Progress and Failure`() = runTest {
        every { rentingRepository.getContract() } returns flowOf(null)

        val emissions = useCase(GetExpensesByVehicleUseCase.Input()).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetExpensesByVehicleUseCase.Output.Progress)
        val failure = emissions[1] as GetExpensesByVehicleUseCase.Output.Failure
        assertEquals(KmError.UnknownError, failure.error)
    }

    @Test
    fun `given contract and empty expenses when invoke then emits Progress and Empty`() = runTest {
        val contract = createContract()
        every { rentingRepository.getContract() } returns flowOf(contract)
        every { expenseRepository.getExpensesByVehicle(contract.id) } returns flowOf(emptyList())
        every { historyRepository.getHistory(contract.id) } returns flowOf(emptyList())

        val emissions = useCase(GetExpensesByVehicleUseCase.Input()).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetExpensesByVehicleUseCase.Output.Progress)
        val empty = emissions[1] as GetExpensesByVehicleUseCase.Output.Empty
        assertEquals(contract.id, empty.vehicleId)
        assertEquals(contract.vehicleName, empty.vehicleName)
        assertEquals(FuelType.GASOLINE_95, empty.defaultFuelType)
    }

    @Test
    fun `given contract and expenses when invoke then emits Progress and Success with totals`() = runTest {
        val contract = createContract()
        val expense1 = createExpense("e1", contract.id, 50.0, 30.0)
        val expense2 = createExpense("e2", contract.id, 70.0, 42.0)
        val tripRecord = OdometerRecord(
            id = "r1",
            contractId = contract.id,
            timestamp = System.currentTimeMillis(),
            odometerValue = 200.0,
            isInitialRecord = false
        )

        every { rentingRepository.getContractById(contract.id) } returns flowOf(contract)
        every { expenseRepository.getExpensesByVehicle(contract.id) } returns flowOf(listOf(expense1, expense2))
        every { historyRepository.getHistory(contract.id) } returns flowOf(listOf(tripRecord))

        val emissions = useCase(GetExpensesByVehicleUseCase.Input(vehicleId = contract.id)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetExpensesByVehicleUseCase.Output.Progress)
        val success = emissions[1] as GetExpensesByVehicleUseCase.Output.Success

        assertEquals(contract.id, success.vehicleId)
        assertEquals(2, success.expenses.size)
        assertEquals(120.0, success.allTimeTotalCost, 0.001)
        assertEquals(120.0, success.currentMonthTotalCost, 0.001)
        assertEquals(72.0, success.currentMonthTotalVolume, 0.001)
        assertEquals(5200.0, success.currentOdometer, 0.001)
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        every { rentingRepository.getContract() } returns flow { throw RuntimeException("Database error") }

        val emissions = useCase(GetExpensesByVehicleUseCase.Input()).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetExpensesByVehicleUseCase.Output.Progress)
        assertTrue(emissions[1] is GetExpensesByVehicleUseCase.Output.Failure)
    }
}
