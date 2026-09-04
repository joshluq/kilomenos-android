package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.BackupData
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.repository.DataManagementRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExportDataUseCaseTest {

    private val repository: DataManagementRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: ExportDataUseCase

    @Before
    fun setUp() {
        useCase = ExportDataUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createBackupData(): BackupData {
        val contract = RentingContract(
            id = "c1",
            userId = "u1",
            vehicleName = "Car",
            startDate = 1000L,
            durationMonths = 12,
            totalKms = 10000.0,
            startOdometer = 0.0,
            currentOdometer = 0.0
        )
        val record = OdometerRecord(
            id = "r1",
            contractId = "c1",
            timestamp = 2000L,
            odometerValue = 50.0,
            isInitialRecord = false
        )
        return BackupData(contracts = listOf(contract), history = listOf(record))
    }

    @Test
    fun `given JSON format when invoke then exports json content and emits Success`() = runTest {
        val backupData = createBackupData()
        every { repository.getFullBackupData() } returns flowOf(backupData)

        val emissions = useCase(ExportDataUseCase.Input(ExportDataUseCase.Format.JSON)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is ExportDataUseCase.Output.Progress)
        val success = emissions[1] as ExportDataUseCase.Output.Success
        assertEquals(ExportDataUseCase.Format.JSON, success.format)
        assertTrue(success.content.contains("Car"))
    }

    @Test
    fun `given CSV format when invoke then generates CSV and emits Success`() = runTest {
        val backupData = createBackupData()
        every { repository.getFullBackupData() } returns flowOf(backupData)
        coEvery { repository.generateHistoryCsv(backupData.history) } returns "id,timestamp,odometerValue\nr1,2000,50.0"

        val emissions = useCase(ExportDataUseCase.Input(ExportDataUseCase.Format.CSV)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is ExportDataUseCase.Output.Progress)
        val success = emissions[1] as ExportDataUseCase.Output.Success
        assertEquals(ExportDataUseCase.Format.CSV, success.format)
        assertTrue(success.content.contains("r1,2000,50.0"))
    }

    @Test
    fun `given error when invoke then catches and emits Failure`() = runTest {
        every { repository.getFullBackupData() } returns flow { throw RuntimeException("Export failed") }

        val emissions = useCase(ExportDataUseCase.Input(ExportDataUseCase.Format.JSON)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is ExportDataUseCase.Output.Progress)
        val failure = emissions[1] as ExportDataUseCase.Output.Failure
        assertEquals("Export failed", failure.message)
    }
}
