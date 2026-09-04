package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
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

class GetFavoriteServiceStationsUseCaseTest {

    private val repository: ServiceStationRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: GetFavoriteServiceStationsUseCase

    @Before
    fun setUp() {
        useCase = GetFavoriteServiceStationsUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createStation(id: String) = ServiceStation(
        id = id,
        name = "Station $id",
        brand = "BP",
        latitude = 40.0,
        longitude = -3.0,
        address = "Street $id",
        isFavorite = true,
        availableEnergies = listOf(FuelType.GASOLINE_98)
    )

    @Test
    fun `given favorite stations when invoke then emits Progress and Success`() = runTest {
        val stations = listOf(createStation("s1"), createStation("s2"))
        every { repository.getFavoriteStations() } returns flowOf(stations)

        val emissions = useCase(GetFavoriteServiceStationsUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetFavoriteServiceStationsUseCase.Output.Progress)
        val success = emissions[1] as GetFavoriteServiceStationsUseCase.Output.Success
        assertEquals(stations, success.stations)
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        every { repository.getFavoriteStations() } returns flow { throw RuntimeException("Error loading") }

        val emissions = useCase(GetFavoriteServiceStationsUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetFavoriteServiceStationsUseCase.Output.Progress)
        assertTrue(emissions[1] is GetFavoriteServiceStationsUseCase.Output.Failure)
    }
}
