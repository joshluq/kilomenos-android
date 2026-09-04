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

class GetAllServiceStationsUseCaseTest {

    private val repository: ServiceStationRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: GetAllServiceStationsUseCase

    @Before
    fun setUp() {
        useCase = GetAllServiceStationsUseCaseImpl(repository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createStation(id: String) = ServiceStation(
        id = id,
        name = "Station $id",
        brand = "Repsol",
        latitude = 40.0,
        longitude = -3.0,
        address = "Address $id",
        isFavorite = false,
        availableEnergies = listOf(FuelType.GASOLINE_95)
    )

    @Test
    fun `given empty stations list when invoke then emits Progress and Empty`() = runTest {
        every { repository.getAllStations() } returns flowOf(emptyList())

        val emissions = useCase(GetAllServiceStationsUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetAllServiceStationsUseCase.Output.Progress)
        assertTrue(emissions[1] is GetAllServiceStationsUseCase.Output.Empty)
    }

    @Test
    fun `given stations list when invoke then emits Progress and Success`() = runTest {
        val stations = listOf(createStation("s1"), createStation("s2"))
        every { repository.getAllStations() } returns flowOf(stations)

        val emissions = useCase(GetAllServiceStationsUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetAllServiceStationsUseCase.Output.Progress)
        val success = emissions[1] as GetAllServiceStationsUseCase.Output.Success
        assertEquals(stations, success.stations)
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        every { repository.getAllStations() } returns flow { throw RuntimeException("Database error") }

        val emissions = useCase(GetAllServiceStationsUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetAllServiceStationsUseCase.Output.Progress)
        assertTrue(emissions[1] is GetAllServiceStationsUseCase.Output.Failure)
    }
}
