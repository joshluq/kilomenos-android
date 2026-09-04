package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.BackupData
import es.joshluq.kmsafe.domain.repository.DataManagementRepository
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImportDataUseCaseTest {

    private val repository: DataManagementRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: ImportDataUseCase

    @Before
    fun setUp() {
        useCase = ImportDataUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given valid JSON content when invoke then restores data and emits Progress then Success`() = runTest {
        val validJson = Json.encodeToString<BackupData>(BackupData(contracts = emptyList(), history = emptyList()))
        every { repository.restoreFromBackup(any()) } returns flowOf(Unit)

        val emissions = useCase(ImportDataUseCase.Input(validJson)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is ImportDataUseCase.Output.Progress)
        assertTrue(emissions[1] is ImportDataUseCase.Output.Success)

        coVerify(exactly = 1) { repository.restoreFromBackup(any()) }
    }

    @Test
    fun `given malformed JSON content when invoke then catches and emits Failure`() = runTest {
        val invalidJson = "{ invalid json content }"

        val emissions = useCase(ImportDataUseCase.Input(invalidJson)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is ImportDataUseCase.Output.Progress)
        assertTrue(emissions[1] is ImportDataUseCase.Output.Failure)
    }
}
