package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.SyncStatus
import es.joshluq.kmsafe.domain.repository.AuthRepository
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
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

class MigrateLocalDataToRemoteUseCaseTest {

    private val rentingRepository: RentingRepository = mockk()
    private val historyRepository: HistoryRepository = mockk(relaxed = true)
    private val fuelRepository: FuelExpenseRepository = mockk()
    private val stationRepository: ServiceStationRepository = mockk()
    private val authRepository: AuthRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: MigrateLocalDataToRemoteUseCase

    @Before
    fun setUp() {
        useCase = MigrateLocalDataToRemoteUseCaseImpl(
            rentingRepository = rentingRepository,
            historyRepository = historyRepository,
            fuelRepository = fuelRepository,
            stationRepository = stationRepository,
            authRepository = authRepository,
            logger = logger
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given free user when invoke then aborts migration early and emits Success`() = runTest {
        val entitlements = Entitlements.Default.copy(subscriptionLevel = SubscriptionLevel.FREE)
        every { authRepository.getEntitlements() } returns flowOf(entitlements)

        val emissions = useCase(MigrateLocalDataToRemoteUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is MigrateLocalDataToRemoteUseCase.Output.Progress)
        assertTrue(emissions[1] is MigrateLocalDataToRemoteUseCase.Output.Success)

        coVerify(exactly = 0) { rentingRepository.getAllContracts() }
    }

    @Test
    fun `given premium user and pending data when invoke then migrates pending data to remote and emits Success`() = runTest {
        val entitlements = Entitlements.Default.copy(subscriptionLevel = SubscriptionLevel.PREMIUM)
        every { authRepository.getEntitlements() } returns flowOf(entitlements)

        val pendingStation = ServiceStation(
            id = "st-1",
            name = "Repsol",
            brand = "Repsol",
            latitude = 40.0,
            longitude = -3.0,
            address = "Gran Via",
            syncStatus = SyncStatus.PENDING
        )
        every { stationRepository.getAllStations() } returns flowOf(listOf(pendingStation))
        every { stationRepository.syncStationBatch(listOf(pendingStation)) } returns flowOf(listOf(pendingStation))

        val pendingContract = RentingContract(
            id = "c1",
            userId = "u1",
            vehicleName = "Car",
            startDate = 1000L,
            durationMonths = 12,
            totalKms = 10000.0,
            startOdometer = 0.0,
            currentOdometer = 0.0,
            syncStatus = SyncStatus.PENDING
        )
        every { rentingRepository.getAllContracts() } returns flowOf(listOf(pendingContract))
        every { rentingRepository.saveContract(pendingContract) } returns flowOf("remote-c1")

        val pendingRecord = OdometerRecord(
            id = "rec-1",
            contractId = "remote-c1",
            timestamp = 2000L,
            odometerValue = 100.0,
            isInitialRecord = false,
            syncStatus = SyncStatus.PENDING
        )
        every { historyRepository.getHistory("remote-c1") } returns flowOf(listOf(pendingRecord))

        val pendingExpense = FuelExpense(
            id = "exp-1",
            vehicleId = "remote-c1",
            timestamp = 2500L,
            fuelType = FuelType.GASOLINE_95,
            unitPrice = 1.5,
            volumeQuantity = 30.0,
            totalCost = 45.0,
            syncStatus = SyncStatus.PENDING
        )
        every { fuelRepository.getExpensesByVehicle("remote-c1") } returns flowOf(listOf(pendingExpense))
        every { fuelRepository.saveExpense(pendingExpense) } returns flowOf("exp-1")

        val emissions = useCase(MigrateLocalDataToRemoteUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is MigrateLocalDataToRemoteUseCase.Output.Progress)
        assertTrue(emissions[1] is MigrateLocalDataToRemoteUseCase.Output.Success)

        coVerify(exactly = 1) { stationRepository.syncStationBatch(listOf(pendingStation)) }
        coVerify(exactly = 1) { rentingRepository.saveContract(pendingContract) }
        coVerify(exactly = 1) { historyRepository.saveRecord(pendingRecord) }
        coVerify(exactly = 1) { fuelRepository.saveExpense(pendingExpense) }
    }

    @Test
    fun `given critical error when invoke then catches and emits Failure`() = runTest {
        every { authRepository.getEntitlements() } returns flow { throw RuntimeException("Auth failure") }

        val emissions = useCase(MigrateLocalDataToRemoteUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is MigrateLocalDataToRemoteUseCase.Output.Progress)
        assertTrue(emissions[1] is MigrateLocalDataToRemoteUseCase.Output.Failure)
    }
}
