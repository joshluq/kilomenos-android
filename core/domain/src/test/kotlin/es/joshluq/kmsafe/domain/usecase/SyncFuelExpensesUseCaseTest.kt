package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
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

class SyncFuelExpensesUseCaseTest {

    private val fuelExpenseRepository: FuelExpenseRepository = mockk()
    private val rentingRepository: RentingRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: SyncFuelExpensesUseCase

    private val sampleContract = RentingContract(
        id = "contract-123",
        vehicleName = "Audi A3",
        startOdometer = 10000.0,
        currentOdometer = 12000.0,
        totalKms = 30000.0,
        durationMonths = 36,
        startDate = 1000L,
        fuelType = FuelType.GASOLINE_95
    )

    private val sampleExpenses = listOf(
        FuelExpense(
            id = "exp-1",
            vehicleId = "contract-123",
            stationId = "st-1",
            stationName = "Repsol",
            timestamp = 1500L,
            fuelType = FuelType.GASOLINE_95,
            unitPrice = 1.65,
            volumeQuantity = 40.0,
            totalCost = 66.0,
            odometerAtExpense = 12000.0,
            isFullTank = true
        )
    )

    @Before
    fun setUp() {
        useCase = SyncFuelExpensesUseCaseImpl(fuelExpenseRepository, rentingRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given active contract when invoke without vehicleId then syncs and emits Progress then Success`() = runTest {
        every { rentingRepository.getContract() } returns flowOf(sampleContract)
        every { fuelExpenseRepository.syncFuelExpenses("contract-123") } returns flowOf(sampleExpenses)

        val emissions = useCase(SyncFuelExpensesUseCase.Input()).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SyncFuelExpensesUseCase.Output.Progress)
        val success = emissions[1] as SyncFuelExpensesUseCase.Output.Success
        assertEquals(1, success.expensesCount)

        coVerify(exactly = 1) { rentingRepository.getContract() }
        coVerify(exactly = 1) { fuelExpenseRepository.syncFuelExpenses("contract-123") }
    }

    @Test
    fun `given explicit vehicleId when invoke then fetches contract by ID and syncs`() = runTest {
        every { rentingRepository.getContractById("custom-vehicle-id") } returns flowOf(sampleContract.copy(id = "custom-vehicle-id"))
        every { fuelExpenseRepository.syncFuelExpenses("custom-vehicle-id") } returns flowOf(emptyList())

        val emissions = useCase(SyncFuelExpensesUseCase.Input(vehicleId = "custom-vehicle-id")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SyncFuelExpensesUseCase.Output.Progress)
        val success = emissions[1] as SyncFuelExpensesUseCase.Output.Success
        assertEquals(0, success.expensesCount)

        coVerify(exactly = 1) { rentingRepository.getContractById("custom-vehicle-id") }
        coVerify(exactly = 1) { fuelExpenseRepository.syncFuelExpenses("custom-vehicle-id") }
    }

    @Test
    fun `given no contract found when invoke then emits Failure with UnknownError`() = runTest {
        every { rentingRepository.getContract() } returns flowOf(null)

        val emissions = useCase(SyncFuelExpensesUseCase.Input()).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SyncFuelExpensesUseCase.Output.Progress)
        val failure = emissions[1] as SyncFuelExpensesUseCase.Output.Failure
        assertEquals(KmError.UnknownError, failure.error)

        coVerify(exactly = 0) { fuelExpenseRepository.syncFuelExpenses(any()) }
    }

    @Test
    fun `given KmException when invoke then emits Failure with corresponding KmError`() = runTest {
        every { rentingRepository.getContract() } returns flowOf(sampleContract)
        every { fuelExpenseRepository.syncFuelExpenses("contract-123") } returns flow {
            throw KmException(KmError.NetworkError)
        }

        val emissions = useCase(SyncFuelExpensesUseCase.Input()).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SyncFuelExpensesUseCase.Output.Progress)
        val failure = emissions[1] as SyncFuelExpensesUseCase.Output.Failure
        assertEquals(KmError.NetworkError, failure.error)
    }

    @Test
    fun `given generic exception when invoke then emits Failure with NetworkError default`() = runTest {
        every { rentingRepository.getContract() } returns flowOf(sampleContract)
        every { fuelExpenseRepository.syncFuelExpenses("contract-123") } returns flow {
            throw IllegalStateException("Something broke")
        }

        val emissions = useCase(SyncFuelExpensesUseCase.Input()).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SyncFuelExpensesUseCase.Output.Progress)
        val failure = emissions[1] as SyncFuelExpensesUseCase.Output.Failure
        assertEquals(KmError.NetworkError, failure.error)
    }
}
