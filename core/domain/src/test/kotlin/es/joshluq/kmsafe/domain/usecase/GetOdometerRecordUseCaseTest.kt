package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetOdometerRecordUseCaseTest {

    private val rentingRepository: RentingRepository = mockk()
    private val historyRepository: HistoryRepository = mockk()

    private lateinit var useCase: GetOdometerRecordUseCase

    @Before
    fun setUp() {
        useCase = GetOdometerRecordUseCaseImpl(rentingRepository, historyRepository)
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
        durationMonths = 12,
        totalKms = 10000.0,
        startOdometer = 0.0,
        currentOdometer = 0.0
    )

    @Test
    fun `given no contract when invoke then emits Progress and Failure`() = runTest {
        every { rentingRepository.getContract() } returns flowOf(null)

        val emissions = useCase(GetOdometerRecordUseCase.Input("rec-1")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetOdometerRecordUseCase.Output.Progress)
        val failure = emissions[1] as GetOdometerRecordUseCase.Output.Failure
        assertEquals("No contract found", failure.message)
    }

    @Test
    fun `given target record not in list when invoke then emits Progress and Failure Record not found`() = runTest {
        val contract = createContract()
        val record = OdometerRecord(id = "rec-other", contractId = contract.id, timestamp = 1000L, odometerValue = 10.0, isInitialRecord = false)

        every { rentingRepository.getContract() } returns flowOf(contract)
        every { historyRepository.getHistory(contract.id) } returns flowOf(listOf(record))

        val emissions = useCase(GetOdometerRecordUseCase.Input("rec-missing")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetOdometerRecordUseCase.Output.Progress)
        val failure = emissions[1] as GetOdometerRecordUseCase.Output.Failure
        assertEquals("Record not found", failure.message)
    }

    @Test
    fun `given first record when invoke then emits Success with null previousRecord`() = runTest {
        val contract = createContract()
        val record1 = OdometerRecord(id = "rec-1", contractId = contract.id, timestamp = 1000L, odometerValue = 10.0, isInitialRecord = false)
        val record2 = OdometerRecord(id = "rec-2", contractId = contract.id, timestamp = 2000L, odometerValue = 20.0, isInitialRecord = false)

        every { rentingRepository.getContract() } returns flowOf(contract)
        every { historyRepository.getHistory(contract.id) } returns flowOf(listOf(record1, record2))

        val emissions = useCase(GetOdometerRecordUseCase.Input("rec-1")).toList()

        assertEquals(2, emissions.size)
        val success = emissions[1] as GetOdometerRecordUseCase.Output.Success
        assertEquals(record1, success.record)
        assertNull(success.previousRecord)
    }

    @Test
    fun `given subsequent record when invoke then emits Success with correct previousRecord`() = runTest {
        val contract = createContract()
        val record1 = OdometerRecord(id = "rec-1", contractId = contract.id, timestamp = 1000L, odometerValue = 10.0, isInitialRecord = false)
        val record2 = OdometerRecord(id = "rec-2", contractId = contract.id, timestamp = 2000L, odometerValue = 20.0, isInitialRecord = false)

        every { rentingRepository.getContract() } returns flowOf(contract)
        every { historyRepository.getHistory(contract.id) } returns flowOf(listOf(record1, record2))

        val emissions = useCase(GetOdometerRecordUseCase.Input("rec-2")).toList()

        assertEquals(2, emissions.size)
        val success = emissions[1] as GetOdometerRecordUseCase.Output.Success
        assertEquals(record2, success.record)
        assertEquals(record1, success.previousRecord)
    }
}
