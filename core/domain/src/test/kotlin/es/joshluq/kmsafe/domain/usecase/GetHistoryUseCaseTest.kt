package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetHistoryUseCaseTest {

    private val historyRepository: HistoryRepository = mockk()
    private val rentingRepository: RentingRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: GetHistoryUseCase

    @Before
    fun setUp() {
        useCase = GetHistoryUseCaseImpl(historyRepository, rentingRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createContract() = RentingContract(
        id = "c1",
        userId = "u1",
        vehicleName = "Golf",
        startDate = 1000L,
        durationMonths = 12,
        totalKms = 12000.0,
        startOdometer = 10000.0,
        currentOdometer = 10300.0
    )

    @Test
    fun `given no active contract when invoke then emits Progress and Empty`() = runTest {
        every { rentingRepository.getContract() } returns flowOf(null)

        val emissions = useCase(GetHistoryUseCase.Input(forceRefresh = false)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetHistoryUseCase.Output.Progress)
        assertTrue(emissions[1] is GetHistoryUseCase.Output.Empty)
    }

    @Test
    fun `given active contract and empty records when invoke then emits Progress and Empty`() = runTest {
        val contract = createContract()
        every { rentingRepository.getContract() } returns flowOf(contract)
        every { historyRepository.getHistory(contract.id) } returns flowOf(emptyList())

        val emissions = useCase(GetHistoryUseCase.Input(forceRefresh = false)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetHistoryUseCase.Output.Progress)
        assertTrue(emissions[1] is GetHistoryUseCase.Output.Empty)
    }

    @Test
    fun `given records when invoke without refresh then excludes initial record from totalKms and emits Success`() = runTest {
        val contract = createContract()
        val initialRecord = OdometerRecord(
            id = "rec-0",
            contractId = contract.id,
            timestamp = 1000L,
            odometerValue = 10000.0,
            isInitialRecord = true
        )
        val trip1 = OdometerRecord(
            id = "rec-1",
            contractId = contract.id,
            timestamp = 2000L,
            odometerValue = 100.0,
            isInitialRecord = false
        )
        val trip2 = OdometerRecord(
            id = "rec-2",
            contractId = contract.id,
            timestamp = 3000L,
            odometerValue = 200.0,
            isInitialRecord = false
        )

        every { rentingRepository.getContract() } returns flowOf(contract)
        every { historyRepository.getHistory(contract.id) } returns flowOf(listOf(initialRecord, trip1, trip2))

        val emissions = useCase(GetHistoryUseCase.Input(forceRefresh = false)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetHistoryUseCase.Output.Progress)
        val success = emissions[1] as GetHistoryUseCase.Output.Success

        assertEquals(contract.id, success.contractId)
        assertEquals(initialRecord, success.initialRecord)
        assertEquals(300.0, success.totalKms, 0.001)
        assertEquals(2, success.totalRecordsCount)
        assertEquals(2, success.allRecords.size)
        // Descending sort order
        assertEquals("rec-2", success.allRecords[0].record.id)
        assertEquals("rec-1", success.allRecords[1].record.id)
    }

    @Test
    fun `given forceRefresh true when invoke then syncs history and emits Success`() = runTest {
        val contract = createContract()
        val record = OdometerRecord(
            id = "rec-1",
            contractId = contract.id,
            timestamp = 2000L,
            odometerValue = 50.0,
            isInitialRecord = false
        )

        every { rentingRepository.getContract() } returns flowOf(contract)
        every { historyRepository.getHistory(contract.id) } returns flowOf(listOf(record))
        every { historyRepository.syncHistory(contract.id) } returns flowOf(Unit)

        val emissions = useCase(GetHistoryUseCase.Input(forceRefresh = true)).toList()

        val success = emissions[1] as GetHistoryUseCase.Output.Success
        assertEquals(50.0, success.totalKms, 0.001)
        coVerify(exactly = 1) { historyRepository.syncHistory(contract.id) }
    }
}
