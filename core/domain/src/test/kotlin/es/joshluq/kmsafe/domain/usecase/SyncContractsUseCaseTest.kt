package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.RentingContract
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

class SyncContractsUseCaseTest {

    private val repository: RentingRepository = mockk()
    private val historyRepository: HistoryRepository = mockk()
    private val stationRepository: ServiceStationRepository = mockk()
    private val fuelRepository: FuelExpenseRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: SyncContractsUseCase

    @Before
    fun setUp() {
        useCase = SyncContractsUseCaseImpl(
            repository = repository,
            historyRepository = historyRepository,
            stationRepository = stationRepository,
            fuelRepository = fuelRepository,
            logger = logger
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createContract(id: String, isSelected: Boolean) = RentingContract(
        id = id,
        userId = "u1",
        vehicleName = "Car",
        startDate = 1000L,
        durationMonths = 12,
        totalKms = 10000.0,
        startOdometer = 0.0,
        currentOdometer = 0.0,
        isSelected = isSelected
    )

    @Test
    fun `given active contract when invoke then deep syncs history and fuel expenses and emits Success`() = runTest {
        val activeContract = createContract("c1", isSelected = true)
        every { repository.syncContracts() } returns flowOf(listOf(activeContract))
        every { stationRepository.syncStations() } returns flowOf(emptyList())
        every { historyRepository.syncHistory("c1") } returns flowOf(Unit)
        every { fuelRepository.syncFuelExpenses("c1") } returns flowOf(emptyList())

        val emissions = useCase(SyncContractsUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SyncContractsUseCase.Output.Progress)
        assertTrue(emissions[1] is SyncContractsUseCase.Output.Success)

        coVerify(exactly = 1) { historyRepository.syncHistory("c1") }
        coVerify(exactly = 1) { fuelRepository.syncFuelExpenses("c1") }
    }

    @Test
    fun `given no active contract when invoke then completes sync without history sync and emits Success`() = runTest {
        val inactiveContract = createContract("c1", isSelected = false)
        every { repository.syncContracts() } returns flowOf(listOf(inactiveContract))
        every { stationRepository.syncStations() } returns flowOf(emptyList())

        val emissions = useCase(SyncContractsUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SyncContractsUseCase.Output.Progress)
        assertTrue(emissions[1] is SyncContractsUseCase.Output.Success)

        coVerify(exactly = 0) { historyRepository.syncHistory(any()) }
    }

    @Test
    fun `given sync error when invoke then catches and emits Failure`() = runTest {
        every { repository.syncContracts() } returns flow { throw RuntimeException("Network error") }
        every { stationRepository.syncStations() } returns flowOf(emptyList())

        val emissions = useCase(SyncContractsUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is SyncContractsUseCase.Output.Progress)
        val failure = emissions[1] as SyncContractsUseCase.Output.Failure
        assertEquals("Network error", failure.message)
    }
}
