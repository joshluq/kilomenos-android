package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateOdometerRecordUseCaseTest {

    private val repository: HistoryRepository = mockk(relaxed = true)

    private lateinit var useCase: UpdateOdometerRecordUseCase

    @Before
    fun setUp() {
        useCase = UpdateOdometerRecordUseCaseImpl(repository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given record when invoke then updates record and emits Progress then Success`() = runTest {
        val record = OdometerRecord(id = "rec-1", contractId = "c1", timestamp = 1000L, odometerValue = 120.0, isInitialRecord = false)

        val emissions = useCase(UpdateOdometerRecordUseCase.Input(record)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is UpdateOdometerRecordUseCase.Output.Progress)
        assertTrue(emissions[1] is UpdateOdometerRecordUseCase.Output.Success)

        coVerify(exactly = 1) { repository.updateRecord(record) }
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        val record = OdometerRecord(id = "rec-1", contractId = "c1", timestamp = 1000L, odometerValue = 120.0, isInitialRecord = false)
        coEvery { repository.updateRecord(record) } throws RuntimeException("DB update failed")

        val emissions = useCase(UpdateOdometerRecordUseCase.Input(record)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is UpdateOdometerRecordUseCase.Output.Progress)
        val failure = emissions[1] as UpdateOdometerRecordUseCase.Output.Failure
        assertEquals("DB update failed", failure.message)
    }
}
