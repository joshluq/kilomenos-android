package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
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

class DeleteServiceStationUseCaseTest {

    private val repository: ServiceStationRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: DeleteServiceStationUseCase

    @Before
    fun setUp() {
        useCase = DeleteServiceStationUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given stationId when invoke then deletes station and emits Progress then Success`() = runTest {
        val stationId = "st-to-delete"
        every { repository.deleteStation(stationId) } returns flowOf(Unit)

        val emissions = useCase(DeleteServiceStationUseCase.Input(stationId)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is DeleteServiceStationUseCase.Output.Progress)
        assertTrue(emissions[1] is DeleteServiceStationUseCase.Output.Success)

        coVerify(exactly = 1) { repository.deleteStation(stationId) }
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        val stationId = "st-to-delete"
        every { repository.deleteStation(stationId) } returns flow { throw RuntimeException("Delete error") }

        val emissions = useCase(DeleteServiceStationUseCase.Input(stationId)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is DeleteServiceStationUseCase.Output.Progress)
        assertTrue(emissions[1] is DeleteServiceStationUseCase.Output.Failure)
    }
}
