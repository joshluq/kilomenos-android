package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.ContractMetrics
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetWidgetSummaryUseCaseTest {

    private val rentingRepository: RentingRepository = mockk()
    private val historyRepository: HistoryRepository = mockk()
    private val calculateContractMetricsUseCase: CalculateContractMetricsUseCase = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: GetWidgetSummaryUseCase

    @Before
    fun setUp() {
        useCase = GetWidgetSummaryUseCaseImpl(
            rentingRepository = rentingRepository,
            historyRepository = historyRepository,
            calculateContractMetricsUseCase = calculateContractMetricsUseCase,
            logger = logger
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createContract(vehicleName: String = "Tesla Model 3") = RentingContract(
        id = "contract-test-1",
        userId = "user-1",
        vehicleName = vehicleName,
        startDate = 1000L,
        durationMonths = 12,
        totalKms = 15000.0,
        startOdometer = 20000.0,
        currentOdometer = 22500.0
    )

    private fun createMetrics(contract: RentingContract, balance: Double = 150.0) = ContractMetrics(
        contract = contract,
        actualKmsDriven = 2500.0,
        currentOdometer = 22500.0,
        theoreticalKms = 2650.0,
        balance = balance,
        dailyBudget = 41.0,
        monthlyBudget = 1250.0,
        timePercentage = 0.2f,
        kmsPercentage = 0.16f,
        differencePercentage = 4.0f,
        isSyncPending = false
    )

    @Test
    fun `when no active contract exists then returns NoActiveContract`() = runTest {
        every { rentingRepository.getContract() } returns flowOf(null)

        val result = useCase(GetWidgetSummaryUseCase.Input)

        assertTrue(result.isSuccess)
        val output = result.getOrNull()
        assertTrue(output is GetWidgetSummaryUseCase.Output.NoActiveContract)
    }

    @Test
    fun `when contract exists and balance is positive then returns Success with isSafe true`() = runTest {
        val contract = createContract()
        val records = emptyList<OdometerRecord>()
        val metrics = createMetrics(contract, balance = 200.0)

        every { rentingRepository.getContract() } returns flowOf(contract)
        every { historyRepository.getHistory(contract.id) } returns flowOf(records)
        coEvery {
            calculateContractMetricsUseCase(any())
        } returns Result.success(CalculateContractMetricsUseCase.Output.Success(metrics))

        val result = useCase(GetWidgetSummaryUseCase.Input)

        assertTrue(result.isSuccess)
        val output = result.getOrNull() as GetWidgetSummaryUseCase.Output.Success
        assertEquals("Tesla Model 3", output.summary.vehicleName)
        assertEquals(200.0, output.summary.balance, 0.01)
        assertEquals(22500.0, output.summary.currentOdometer, 0.01)
        assertEquals(41.0, output.summary.dailyBudget, 0.01)
        assertTrue(output.summary.isSafe)
        assertFalse(output.summary.isSyncPending)
    }

    @Test
    fun `when contract exists and balance is negative then returns Success with isSafe false`() = runTest {
        val contract = createContract()
        val records = emptyList<OdometerRecord>()
        val metrics = createMetrics(contract, balance = -80.0)

        every { rentingRepository.getContract() } returns flowOf(contract)
        every { historyRepository.getHistory(contract.id) } returns flowOf(records)
        coEvery {
            calculateContractMetricsUseCase(any())
        } returns Result.success(CalculateContractMetricsUseCase.Output.Success(metrics))

        val result = useCase(GetWidgetSummaryUseCase.Input)

        assertTrue(result.isSuccess)
        val output = result.getOrNull() as GetWidgetSummaryUseCase.Output.Success
        assertEquals(-80.0, output.summary.balance, 0.01)
        assertFalse(output.summary.isSafe)
    }
}
