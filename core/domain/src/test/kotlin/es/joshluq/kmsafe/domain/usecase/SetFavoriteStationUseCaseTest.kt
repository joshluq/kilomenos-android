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

class SetFavoriteStationUseCaseTest {

    private val repository: ServiceStationRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: SetFavoriteStationUseCase

    @Before
    fun setUp() {
        useCase = SetFavoriteStationUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given stationId and isFavorite when invoke then sets favorite and emits Progress then Success`() = runTest {
        val stationId = "st-1"
        every { repository.setFavorite(stationId, true) } returns flowOf(Unit)

        val emissions = useCase(SetFavoriteStationUseCase.Input(stationId, isFavorite = true)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SetFavoriteStationUseCase.Output.Progress)
        assertTrue(emissions[1] is SetFavoriteStationUseCase.Output.Success)

        coVerify(exactly = 1) { repository.setFavorite(stationId, true) }
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        val stationId = "st-1"
        every { repository.setFavorite(stationId, false) } returns flow { throw RuntimeException("Error") }

        val emissions = useCase(SetFavoriteStationUseCase.Input(stationId, isFavorite = false)).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SetFavoriteStationUseCase.Output.Progress)
        assertTrue(emissions[1] is SetFavoriteStationUseCase.Output.Failure)
    }
}
