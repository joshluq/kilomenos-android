package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.SyncStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalculateContractMetricsUseCaseTest {

    private lateinit var useCase: CalculateContractMetricsUseCase

    @Before
    fun setUp() {
        useCase = CalculateContractMetricsUseCaseImpl()
    }

    private fun createContract(
        startDate: Long = 1_000_000_000L,
        durationMonths: Int = 12,
        totalKms: Double = 12_000.0,
        startOdometer: Double = 10_000.0,
        syncStatus: SyncStatus = SyncStatus.SYNCED
    ) = RentingContract(
        id = "contract-1",
        userId = "user-1",
        vehicleName = "Test Car",
        startDate = startDate,
        durationMonths = durationMonths,
        totalKms = totalKms,
        startOdometer = startOdometer,
        currentOdometer = startOdometer,
        syncStatus = syncStatus
    )

    @Test
    fun `brand new contract on day 0 has zero balance and start odometer`() = runTest {
        val startDate = 1_000_000_000L
        val contract = createContract(startDate = startDate, startOdometer = 10_000.0)
        val initialRecord = OdometerRecord(
            id = "rec-0",
            contractId = contract.id,
            timestamp = startDate,
            odometerValue = 10_000.0,
            isInitialRecord = true,
            syncStatus = SyncStatus.SYNCED
        )

        val result = useCase(
            CalculateContractMetricsUseCase.Input(
                contract = contract,
                records = listOf(initialRecord),
                currentTime = startDate
            )
        )
        val output = result.getOrThrow() as CalculateContractMetricsUseCase.Output.Success
        val metrics = output.metrics

        assertEquals(0.0, metrics.actualKmsDriven, 0.0001)
        assertEquals(10_000.0, metrics.currentOdometer, 0.0001)
        assertEquals(0.0, metrics.theoreticalKms, 0.0001)
        assertEquals(0.0, metrics.balance, 0.0001)
        assertEquals(0.0f, metrics.timePercentage, 0.0001f)
        assertEquals(0.0f, metrics.kmsPercentage, 0.0001f)
        assertEquals(0.0f, metrics.differencePercentage, 0.0001f)
        assertFalse(metrics.isSyncPending)
    }

    @Test
    fun `positive balance when user drives less than daily budget`() = runTest {
        val startDate = 1_000_000_000L
        val tenDaysMillis = 10 * CalculateContractMetricsUseCase.MILLIS_IN_DAY
        val currentTime = startDate + tenDaysMillis
        val contract = createContract(
            startDate = startDate,
            durationMonths = 12,
            totalKms = 12_000.0,
            startOdometer = 10_000.0
        )

        val records = listOf(
            OdometerRecord(
                id = "r0",
                contractId = contract.id,
                timestamp = startDate,
                odometerValue = 10_000.0,
                isInitialRecord = true
            ),
            OdometerRecord(
                id = "r1",
                contractId = contract.id,
                timestamp = startDate + 1000,
                odometerValue = 100.0,
                isInitialRecord = false
            ),
            OdometerRecord(
                id = "r2",
                contractId = contract.id,
                timestamp = startDate + 2000,
                odometerValue = 50.0,
                isInitialRecord = false
            )
        )

        val result = useCase(
            CalculateContractMetricsUseCase.Input(
                contract = contract,
                records = records,
                currentTime = currentTime
            )
        )
        val output = result.getOrThrow() as CalculateContractMetricsUseCase.Output.Success
        val metrics = output.metrics

        // totalDays = 12 * 30.4375 = 365.25
        val expectedDailyBudget = 12_000.0 / 365.25
        val expectedTheoretical = 10.0 * expectedDailyBudget
        val expectedActual = 150.0
        val expectedBalance = expectedTheoretical - expectedActual

        assertEquals(expectedActual, metrics.actualKmsDriven, 0.0001)
        assertEquals(10_150.0, metrics.currentOdometer, 0.0001)
        assertEquals(expectedDailyBudget, metrics.dailyBudget, 0.0001)
        assertEquals(1000.0, metrics.monthlyBudget, 0.0001)
        assertEquals(expectedTheoretical, metrics.theoreticalKms, 0.0001)
        assertEquals(expectedBalance, metrics.balance, 0.0001)
        assertTrue(metrics.balance > 0)
    }

    @Test
    fun `negative balance when user drives more than daily budget`() = runTest {
        val startDate = 1_000_000_000L
        val tenDaysMillis = 10 * CalculateContractMetricsUseCase.MILLIS_IN_DAY
        val currentTime = startDate + tenDaysMillis
        val contract = createContract(
            startDate = startDate,
            durationMonths = 12,
            totalKms = 12_000.0,
            startOdometer = 10_000.0
        )

        val records = listOf(
            OdometerRecord(
                id = "r0",
                contractId = contract.id,
                timestamp = startDate,
                odometerValue = 10_000.0,
                isInitialRecord = true
            ),
            OdometerRecord(
                id = "r1",
                contractId = contract.id,
                timestamp = startDate + 1000,
                odometerValue = 800.0,
                isInitialRecord = false
            )
        )

        val result = useCase(
            CalculateContractMetricsUseCase.Input(
                contract = contract,
                records = records,
                currentTime = currentTime
            )
        )
        val output = result.getOrThrow() as CalculateContractMetricsUseCase.Output.Success
        val metrics = output.metrics

        val expectedDailyBudget = 12_000.0 / (12 * 30.4375)
        val expectedTheoretical = 10.0 * expectedDailyBudget
        val expectedActual = 800.0
        val expectedBalance = expectedTheoretical - expectedActual

        assertEquals(expectedActual, metrics.actualKmsDriven, 0.0001)
        assertEquals(10_800.0, metrics.currentOdometer, 0.0001)
        assertEquals(expectedBalance, metrics.balance, 0.0001)
        assertTrue(metrics.balance < 0)
    }

    @Test
    fun `pending sync is detected from records or contract`() = runTest {
        val contract = createContract(syncStatus = SyncStatus.SYNCED)
        val pendingRecord = OdometerRecord(
            id = "r1",
            contractId = contract.id,
            timestamp = 1000L,
            odometerValue = 50.0,
            isInitialRecord = false,
            syncStatus = SyncStatus.PENDING
        )

        val resultWithPendingRecord = useCase(
            CalculateContractMetricsUseCase.Input(
                contract = contract,
                records = listOf(pendingRecord)
            )
        )
        val metricsWithPendingRecord =
            (resultWithPendingRecord.getOrThrow() as CalculateContractMetricsUseCase.Output.Success).metrics
        assertTrue(metricsWithPendingRecord.isSyncPending)

        val pendingContract = createContract(syncStatus = SyncStatus.PENDING)
        val syncedRecord = OdometerRecord(
            id = "r2",
            contractId = contract.id,
            timestamp = 1000L,
            odometerValue = 50.0,
            isInitialRecord = false,
            syncStatus = SyncStatus.SYNCED
        )
        val resultWithPendingContract = useCase(
            CalculateContractMetricsUseCase.Input(
                contract = pendingContract,
                records = listOf(syncedRecord)
            )
        )
        val metricsWithPendingContract =
            (resultWithPendingContract.getOrThrow() as CalculateContractMetricsUseCase.Output.Success).metrics
        assertTrue(metricsWithPendingContract.isSyncPending)
    }
}
