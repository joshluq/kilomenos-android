package es.joshluq.kmsafe.core.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.core.domain.service.StationNotificationService
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

class HandleGeofenceTransitionUseCaseTest {

    private val stationRepository: ServiceStationRepository = mockk()
    private val notificationService: StationNotificationService = mockk(relaxed = true)
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: HandleGeofenceTransitionUseCase

    @Before
    fun setUp() {
        HandleGeofenceTransitionUseCaseImpl.lastNotifiedTimestamps.clear()
        useCase = HandleGeofenceTransitionUseCaseImpl(
            stationRepository = stationRepository,
            notificationService = notificationService,
            checkFeatureAccessUseCase = checkFeatureAccessUseCase,
            logger = logger
        )
    }

    @After
    fun tearDown() {
        HandleGeofenceTransitionUseCaseImpl.lastNotifiedTimestamps.clear()
        clearAllMocks()
    }

    private fun createStation(id: String) = ServiceStation(
        id = id,
        name = "Repsol Station",
        brand = "Repsol",
        latitude = 40.4168,
        longitude = -3.7038,
        address = "Gran Via 1",
        isFavorite = true,
        availableEnergies = listOf(FuelType.GASOLINE_95)
    )

    @Test
    fun `given feature access not granted when invoke then emits Failure and does not show prompt`() = runTest {
        every {
            checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.STATION_AUTO_DETECTION))
        } returns flowOf(CheckFeatureAccessUseCase.Output.Success(isGranted = false))

        val emissions = useCase(HandleGeofenceTransitionUseCase.Input("station-1")).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0] is HandleGeofenceTransitionUseCase.Output.Failure)
        coVerify(exactly = 0) { notificationService.showStationProximityPrompt(any()) }
    }

    @Test
    fun `given feature access granted but station not found when invoke then emits Failure`() = runTest {
        every {
            checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.STATION_AUTO_DETECTION))
        } returns flowOf(CheckFeatureAccessUseCase.Output.Success(isGranted = true))
        every { stationRepository.getStationById("station-missing") } returns flowOf(null)

        val emissions = useCase(HandleGeofenceTransitionUseCase.Input("station-missing")).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0] is HandleGeofenceTransitionUseCase.Output.Failure)
        coVerify(exactly = 0) { notificationService.showStationProximityPrompt(any()) }
    }

    @Test
    fun `given feature access granted and station exists when invoke then shows prompt and emits Success`() = runTest {
        val station = createStation("station-1")
        every {
            checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.STATION_AUTO_DETECTION))
        } returns flowOf(CheckFeatureAccessUseCase.Output.Success(isGranted = true))
        every { stationRepository.getStationById("station-1") } returns flowOf(station)

        val emissions = useCase(HandleGeofenceTransitionUseCase.Input("station-1")).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0] is HandleGeofenceTransitionUseCase.Output.Success)
        coVerify(exactly = 1) { notificationService.showStationProximityPrompt(station) }
    }

    @Test
    fun `given repeated trigger within cooldown when invoke then does not show prompt again`() = runTest {
        val station = createStation("station-1")
        every {
            checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.STATION_AUTO_DETECTION))
        } returns flowOf(CheckFeatureAccessUseCase.Output.Success(isGranted = true))
        every { stationRepository.getStationById("station-1") } returns flowOf(station)

        // First trigger -> shows prompt
        val firstEmissions = useCase(HandleGeofenceTransitionUseCase.Input("station-1")).toList()
        assertEquals(1, firstEmissions.size)
        assertTrue(firstEmissions[0] is HandleGeofenceTransitionUseCase.Output.Success)
        coVerify(exactly = 1) { notificationService.showStationProximityPrompt(station) }

        // Second trigger immediately -> within cooldown -> prompt not called again
        val secondEmissions = useCase(HandleGeofenceTransitionUseCase.Input("station-1")).toList()
        assertEquals(1, secondEmissions.size)
        assertTrue(secondEmissions[0] is HandleGeofenceTransitionUseCase.Output.Success)
        coVerify(exactly = 1) { notificationService.showStationProximityPrompt(station) }
    }

    @Test
    fun `given exception when invoke then catches and emits Failure`() = runTest {
        every {
            checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.STATION_AUTO_DETECTION))
        } returns flow { throw RuntimeException("Feature check error") }

        val emissions = useCase(HandleGeofenceTransitionUseCase.Input("station-1")).toList()

        assertEquals(1, emissions.size)
        assertTrue(emissions[0] is HandleGeofenceTransitionUseCase.Output.Failure)
    }
}
