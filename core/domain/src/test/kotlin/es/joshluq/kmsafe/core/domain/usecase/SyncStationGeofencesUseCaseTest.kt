package es.joshluq.kmsafe.core.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.core.domain.service.GeofenceService
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
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

class SyncStationGeofencesUseCaseTest {

    private val stationRepository: ServiceStationRepository = mockk()
    private val geofenceService: GeofenceService = mockk()
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: SyncStationGeofencesUseCase

    @Before
    fun setUp() {
        useCase = SyncStationGeofencesUseCaseImpl(
            stationRepository = stationRepository,
            geofenceService = geofenceService,
            checkFeatureAccessUseCase = checkFeatureAccessUseCase,
            logger = logger
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createStation(id: String) = ServiceStation(
        id = id,
        name = "Station $id",
        brand = "Cepsa",
        latitude = 40.0,
        longitude = -3.0,
        address = "Street $id",
        isFavorite = true,
        availableEnergies = listOf(FuelType.DIESEL)
    )

    @Test
    fun `given free tier user when invoke then clears all geofences and emits Progress then Success`() = runTest {
        every {
            checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.STATION_AUTO_DETECTION))
        } returns flowOf(CheckFeatureAccessUseCase.Output.Success(isGranted = false))
        every { geofenceService.clearAllGeofences() } returns flowOf(Unit)

        val emissions = useCase(SyncStationGeofencesUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SyncStationGeofencesUseCase.Output.Progress)
        assertTrue(emissions[1] is SyncStationGeofencesUseCase.Output.Success)

        coVerify(exactly = 1) { geofenceService.clearAllGeofences() }
        coVerify(exactly = 0) { geofenceService.registerStationGeofences(any()) }
    }

    @Test
    fun `given premium user with favorite stations when invoke then registers geofences and emits Progress then Success`() = runTest {
        val favorites = listOf(createStation("s1"), createStation("s2"))
        every {
            checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.STATION_AUTO_DETECTION))
        } returns flowOf(CheckFeatureAccessUseCase.Output.Success(isGranted = true))
        every { stationRepository.getFavoriteStations() } returns flowOf(favorites)
        every { geofenceService.registerStationGeofences(favorites) } returns flowOf(Unit)

        val emissions = useCase(SyncStationGeofencesUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SyncStationGeofencesUseCase.Output.Progress)
        assertTrue(emissions[1] is SyncStationGeofencesUseCase.Output.Success)

        coVerify(exactly = 1) { geofenceService.registerStationGeofences(favorites) }
        coVerify(exactly = 0) { geofenceService.clearAllGeofences() }
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        every {
            checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.STATION_AUTO_DETECTION))
        } returns flowOf(CheckFeatureAccessUseCase.Output.Success(isGranted = true))
        every { stationRepository.getFavoriteStations() } returns flow { throw RuntimeException("Sync geofences error") }

        val emissions = useCase(SyncStationGeofencesUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SyncStationGeofencesUseCase.Output.Progress)
        assertTrue(emissions[1] is SyncStationGeofencesUseCase.Output.Failure)
    }
}
