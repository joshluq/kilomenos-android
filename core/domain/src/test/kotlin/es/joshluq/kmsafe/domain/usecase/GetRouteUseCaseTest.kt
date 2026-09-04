package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.TripRoute
import es.joshluq.kmsafe.domain.repository.HistoryRepository
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

class GetRouteUseCaseTest {

    private val repository: HistoryRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: GetRouteUseCase

    @Before
    fun setUp() {
        useCase = GetRouteUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given existing route when invoke then emits Progress and Success`() = runTest {
        val route = TripRoute(recordId = "rec-1", encodedPolyline = "abc_def", pointCount = 10)
        every { repository.getRoute("rec-1") } returns flowOf(route)

        val emissions = useCase(GetRouteUseCase.Input("rec-1")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetRouteUseCase.Output.Progress)
        val success = emissions[1] as GetRouteUseCase.Output.Success
        assertEquals(route, success.route)
    }

    @Test
    fun `given no route when invoke then emits Progress and Failure`() = runTest {
        every { repository.getRoute("rec-1") } returns flowOf(null)

        val emissions = useCase(GetRouteUseCase.Input("rec-1")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetRouteUseCase.Output.Progress)
        val failure = emissions[1] as GetRouteUseCase.Output.Failure
        assertEquals("No route found for this record", failure.message)
    }

    @Test
    fun `given error when invoke then catches and emits Failure`() = runTest {
        every { repository.getRoute("rec-1") } returns flow { throw RuntimeException("Route error") }

        val emissions = useCase(GetRouteUseCase.Input("rec-1")).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetRouteUseCase.Output.Progress)
        val failure = emissions[1] as GetRouteUseCase.Output.Failure
        assertEquals("Route error", failure.message)
    }
}
