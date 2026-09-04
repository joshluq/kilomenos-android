package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ObserveStationRadarUseCaseTest {

    private val stationRepository: ServiceStationRepository = mockk()
    private val expenseRepository: FuelExpenseRepository = mockk()
    private val rentingRepository: RentingRepository = mockk()
    private val entitlementsRepository: EntitlementsRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: ObserveStationRadarUseCase

    private val sampleContract = RentingContract(
        id = "contract-1",
        vehicleName = "Audi A3",
        startDate = 1000L,
        durationMonths = 36,
        totalKms = 30000.0,
        startOdometer = 0.0,
        currentOdometer = 5000.0,
        fuelType = FuelType.DIESEL
    )

    private val sampleStation = ServiceStation(
        id = "station-1",
        name = "Repsol M-40",
        brand = "Repsol",
        latitude = 40.4168,
        longitude = -3.7038,
        address = "Av. M-40 km 12",
        isFavorite = true
    )

    @Before
    fun setUp() {
        useCase = ObserveStationRadarUseCaseImpl(
            stationRepository = stationRepository,
            expenseRepository = expenseRepository,
            rentingRepository = rentingRepository,
            entitlementsRepository = entitlementsRepository,
            logger = logger
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given free user without station radar feature when invoke then emits Success with isLocked true`() = runTest {
        every { rentingRepository.getContract() } returns flowOf(sampleContract)
        every { stationRepository.getAllStations() } returns flowOf(listOf(sampleStation))
        every { expenseRepository.getExpensesByVehicle(sampleContract.id) } returns flowOf(emptyList())
        every { entitlementsRepository.observeEntitlements() } returns flowOf(Entitlements.Default) // Locked

        val emissions = useCase(ObserveStationRadarUseCase.Input()).toList()

        assertEquals(1, emissions.size)
        val success = emissions[0] as ObserveStationRadarUseCase.Output.Success
        assertTrue(success.isLocked)
        assertTrue(success.items.isEmpty())
    }

    @Test
    fun `given unlocked radar when station has expenses then computes delta and opportunity status`() = runTest {
        val unlockedEntitlements = Entitlements.Default.copy(
            subscriptionLevel = SubscriptionLevel.PREMIUM,
            enabledFeatures = setOf(Feature.STATION_PRICE_RADAR)
        )
        val expense1 = FuelExpense(
            id = "exp-1",
            vehicleId = sampleContract.id,
            stationId = sampleStation.id,
            timestamp = 1000L,
            fuelType = FuelType.DIESEL,
            unitPrice = 1.60,
            volumeQuantity = 40.0,
            totalCost = 64.0
        )
        val expense2 = FuelExpense(
            id = "exp-2",
            vehicleId = sampleContract.id,
            stationId = sampleStation.id,
            timestamp = 2000L,
            fuelType = FuelType.DIESEL,
            unitPrice = 1.50, // 10 cents cheaper than expense1!
            volumeQuantity = 40.0,
            totalCost = 60.0
        )

        every { rentingRepository.getContract() } returns flowOf(sampleContract)
        every { stationRepository.getAllStations() } returns flowOf(listOf(sampleStation))
        every { expenseRepository.getExpensesByVehicle(sampleContract.id) } returns flowOf(listOf(expense1, expense2))
        every { entitlementsRepository.observeEntitlements() } returns flowOf(unlockedEntitlements)

        val emissions = useCase(ObserveStationRadarUseCase.Input()).toList()

        assertEquals(1, emissions.size)
        val success = emissions[0] as ObserveStationRadarUseCase.Output.Success
        assertFalse(success.isLocked)
        assertEquals(1, success.items.size)

        val radarItem = success.items[0]
        assertEquals("Repsol M-40", radarItem.station.name)
        assertEquals(1.50, radarItem.lastRecordedPrice, 0.001)
        // Average price = (1.60 + 1.50) / 2 = 1.55
        assertEquals(1.55, radarItem.userAveragePrice, 0.001)
        // Delta = 1.50 - 1.55 = -0.05
        assertEquals(-0.05, radarItem.priceDelta, 0.001)
        assertTrue(radarItem.isOpportunity)
    }
}
