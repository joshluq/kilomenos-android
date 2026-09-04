package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RentingContract
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
import java.util.Calendar

class GetMonthlyUsageUseCaseTest {

    private val historyRepository: HistoryRepository = mockk()
    private val rentingRepository: RentingRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: GetMonthlyUsageUseCase

    @Before
    fun setUp() {
        useCase = GetMonthlyUsageUseCaseImpl(historyRepository, rentingRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createContract() = RentingContract(
        id = "c1",
        userId = "u1",
        vehicleName = "Car",
        startDate = 1000L,
        durationMonths = 10,
        totalKms = 10000.0,
        startOdometer = 0.0,
        currentOdometer = 0.0
    )

    @Test
    fun `given no active contract when invoke then emits Progress and Success with empty list`() = runTest {
        every { rentingRepository.getContract() } returns flowOf(null)

        val emissions = useCase(GetMonthlyUsageUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetMonthlyUsageUseCase.Output.Progress)
        val success = emissions[1] as GetMonthlyUsageUseCase.Output.Success
        assertTrue(success.aggregations.isEmpty())
    }

    @Test
    fun `given contract and empty records when invoke then emits Progress and Success with empty list`() = runTest {
        val contract = createContract()
        every { rentingRepository.getContract() } returns flowOf(contract)
        every { historyRepository.getHistory(contract.id) } returns flowOf(emptyList())

        val emissions = useCase(GetMonthlyUsageUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetMonthlyUsageUseCase.Output.Progress)
        val success = emissions[1] as GetMonthlyUsageUseCase.Output.Success
        assertTrue(success.aggregations.isEmpty())
    }

    @Test
    fun `given records when invoke then aggregates monthly usage excluding initial record`() = runTest {
        val contract = createContract()

        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.JANUARY, 15, 10, 0, 0)
        val janTime1 = cal.timeInMillis
        cal.set(2026, Calendar.JANUARY, 20, 10, 0, 0)
        val janTime2 = cal.timeInMillis
        cal.set(2026, Calendar.FEBRUARY, 5, 10, 0, 0)
        val febTime = cal.timeInMillis

        val initialRec = OdometerRecord(id = "r0", contractId = contract.id, timestamp = janTime1, odometerValue = 1000.0, isInitialRecord = true)
        val janRec1 = OdometerRecord(id = "r1", contractId = contract.id, timestamp = janTime1, odometerValue = 150.0, isInitialRecord = false)
        val janRec2 = OdometerRecord(id = "r2", contractId = contract.id, timestamp = janTime2, odometerValue = 50.0, isInitialRecord = false)
        val febRec = OdometerRecord(id = "r3", contractId = contract.id, timestamp = febTime, odometerValue = 300.0, isInitialRecord = false)

        every { rentingRepository.getContract() } returns flowOf(contract)
        every { historyRepository.getHistory(contract.id) } returns flowOf(listOf(initialRec, janRec1, janRec2, febRec))

        val emissions = useCase(GetMonthlyUsageUseCase.Input).toList()

        assertEquals(2, emissions.size)
        val success = emissions[1] as GetMonthlyUsageUseCase.Output.Success
        val aggregations = success.aggregations

        assertEquals(2, aggregations.size)
        assertEquals(2026, aggregations[0].year)
        assertEquals(Calendar.JANUARY, aggregations[0].month)
        assertEquals(200.0, aggregations[0].totalKms, 0.001)
        assertEquals(1000.0, aggregations[0].budgetedKms, 0.001)

        assertEquals(2026, aggregations[1].year)
        assertEquals(Calendar.FEBRUARY, aggregations[1].month)
        assertEquals(300.0, aggregations[1].totalKms, 0.001)
        assertEquals(1000.0, aggregations[1].budgetedKms, 0.001)
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        every { rentingRepository.getContract() } returns flow { throw RuntimeException("Error loading") }

        val emissions = useCase(GetMonthlyUsageUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetMonthlyUsageUseCase.Output.Progress)
        assertTrue(emissions[1] is GetMonthlyUsageUseCase.Output.Failure)
    }
}
