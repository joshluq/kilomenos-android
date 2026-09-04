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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetOverviewDataUseCaseTest {

    private val rentingRepository: RentingRepository = mockk()
    private val historyRepository: HistoryRepository = mockk()
    private val calculateContractMetricsUseCase: CalculateContractMetricsUseCase = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: GetOverviewDataUseCase

    @Before
    fun setUp() {
        useCase = GetOverviewDataUseCaseImpl(
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

    private fun createContract() = RentingContract(
        id = "contract-1",
        userId = "user-1",
        vehicleName = "Audi A3",
        startDate = 1000L,
        durationMonths = 12,
        totalKms = 12000.0,
        startOdometer = 10000.0,
        currentOdometer = 10500.0
    )

    private fun createMetrics(contract: RentingContract) = ContractMetrics(
        contract = contract,
        actualKmsDriven = 500.0,
        currentOdometer = 10500.0,
        theoreticalKms = 600.0,
        balance = 100.0,
        dailyBudget = 32.8,
        monthlyBudget = 1000.0,
        timePercentage = 0.1f,
        kmsPercentage = 0.05f,
        differencePercentage = 5.0f,
        isSyncPending = false
    )

    @Test
    fun `given no active contract when invoke then emits Progress and Success with null contract`() = runTest {
        every { rentingRepository.getContract() } returns flowOf(null)

        val emissions = useCase(GetOverviewDataUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetOverviewDataUseCase.Output.Progress)
        val success = emissions[1] as GetOverviewDataUseCase.Output.Success
        assertNull(success.contract)
        assertEquals(0.0, success.actualKmsDrivenSinceStart, 0.001)
    }

    @Test
    fun `given active contract and records when invoke then calculates metrics and emits Success`() = runTest {
        val contract = createContract()
        val record = OdometerRecord(id = "r1", contractId = contract.id, timestamp = 2000L, odometerValue = 500.0, isInitialRecord = false)
        val metrics = createMetrics(contract)

        every { rentingRepository.getContract() } returns flowOf(contract)
        every { historyRepository.getHistory(contract.id) } returns flowOf(listOf(record))
        coEvery {
            calculateContractMetricsUseCase(match { it.contract == contract && it.records == listOf(record) })
        } returns Result.success(CalculateContractMetricsUseCase.Output.Success(metrics))

        val emissions = useCase(GetOverviewDataUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetOverviewDataUseCase.Output.Progress)
        val success = emissions[1] as GetOverviewDataUseCase.Output.Success
        assertEquals(contract, success.contract)
        assertEquals(500.0, success.actualKmsDrivenSinceStart, 0.001)
        assertEquals(metrics, success.metrics)
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        every { rentingRepository.getContract() } returns flow { throw RuntimeException("Error reading contract") }

        val emissions = useCase(GetOverviewDataUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetOverviewDataUseCase.Output.Progress)
        assertTrue(emissions[1] is GetOverviewDataUseCase.Output.Failure)
    }
}
