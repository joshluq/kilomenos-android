package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DetectNearestStationUseCaseTest {

    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase = mockk()
    private val stationRepository: ServiceStationRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: DetectNearestStationUseCase

    // Reference location: Madrid Puerta del Sol (40.416775, -3.703790)
    private val refLat = 40.416775
    private val refLng = -3.703790

    @Before
    fun setUp() {
        useCase = DetectNearestStationUseCaseImpl(
            checkFeatureAccessUseCase = checkFeatureAccessUseCase,
            stationRepository = stationRepository,
            logger = logger
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given feature access denied when detecting nearest station then returns AccessDenied`() = runTest {
        every {
            checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.STATION_AUTO_DETECTION))
        } returns flowOf(CheckFeatureAccessUseCase.Output.Success(isGranted = false))

        val result = useCase(DetectNearestStationUseCase.Input(refLat, refLng)).first()

        assertEquals(DetectNearestStationUseCase.Output.AccessDenied, result)
    }

    @Test
    fun `given no stations in bounding box when detecting nearest station then returns NotFound`() = runTest {
        every {
            checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.STATION_AUTO_DETECTION))
        } returns flowOf(CheckFeatureAccessUseCase.Output.Success(isGranted = true))

        every {
            stationRepository.getStationsInBoundingBox(any(), any(), any(), any())
        } returns flowOf(emptyList())

        val result = useCase(DetectNearestStationUseCase.Input(refLat, refLng)).first()

        assertEquals(DetectNearestStationUseCase.Output.NotFound, result)
    }

    @Test
    fun `given station within 50m when detecting nearest station then returns Found`() = runTest {
        every {
            checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.STATION_AUTO_DETECTION))
        } returns flowOf(CheckFeatureAccessUseCase.Output.Success(isGranted = true))

        // Station ~45 meters north (+0.0004 deg lat)
        val station = ServiceStation(
            id = "station-1",
            name = "Repsol Sol",
            brand = "Repsol",
            latitude = refLat + 0.0004,
            longitude = refLng,
            address = "Calle Mayor 1",
            availableEnergies = listOf(FuelType.GASOLINE_95)
        )

        every {
            stationRepository.getStationsInBoundingBox(any(), any(), any(), any())
        } returns flowOf(listOf(station))

        val result = useCase(DetectNearestStationUseCase.Input(refLat, refLng, maxRadiusMeters = 120.0)).first()

        assertTrue(result is DetectNearestStationUseCase.Output.Found)
        val found = result as DetectNearestStationUseCase.Output.Found
        assertEquals("station-1", found.station.id)
        assertTrue("Distance should be around 45m", found.distanceMeters in 40.0..50.0)
    }

    @Test
    fun `given station outside 120m when detecting nearest station then returns NotFound`() = runTest {
        every {
            checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.STATION_AUTO_DETECTION))
        } returns flowOf(CheckFeatureAccessUseCase.Output.Success(isGranted = true))

        // Station ~220 meters north (+0.002 deg lat)
        val station = ServiceStation(
            id = "station-far",
            name = "Far Station",
            brand = "BP",
            latitude = refLat + 0.002,
            longitude = refLng,
            address = "Gran Via 100",
            availableEnergies = listOf(FuelType.DIESEL)
        )

        every {
            stationRepository.getStationsInBoundingBox(any(), any(), any(), any())
        } returns flowOf(listOf(station))

        val result = useCase(DetectNearestStationUseCase.Input(refLat, refLng, maxRadiusMeters = 120.0)).first()

        assertEquals(DetectNearestStationUseCase.Output.NotFound, result)
    }

    @Test
    fun `given multiple stations within range when detecting then selects the closest one`() = runTest {
        every {
            checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.STATION_AUTO_DETECTION))
        } returns flowOf(CheckFeatureAccessUseCase.Output.Success(isGranted = true))

        val stationClose = ServiceStation(
            id = "station-close",
            name = "Closest Station",
            brand = "Repsol",
            latitude = refLat + 0.0002, // ~22m
            longitude = refLng,
            address = "Calle A",
            availableEnergies = listOf(FuelType.GASOLINE_95)
        )

        val stationFurther = ServiceStation(
            id = "station-further",
            name = "Further Station",
            brand = "Cepsa",
            latitude = refLat + 0.0008, // ~89m
            longitude = refLng,
            address = "Calle B",
            availableEnergies = listOf(FuelType.DIESEL)
        )

        every {
            stationRepository.getStationsInBoundingBox(any(), any(), any(), any())
        } returns flowOf(listOf(stationFurther, stationClose))

        val result = useCase(DetectNearestStationUseCase.Input(refLat, refLng, maxRadiusMeters = 120.0)).first()

        assertTrue(result is DetectNearestStationUseCase.Output.Found)
        val found = result as DetectNearestStationUseCase.Output.Found
        assertEquals("station-close", found.station.id)
    }
}
