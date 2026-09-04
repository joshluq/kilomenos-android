package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetTripProjectionUseCaseTest {

    private val rentingRepository: RentingRepository = mockk()
    private val historyRepository: HistoryRepository = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var useCase: GetTripProjectionUseCase

    @Before
    fun setUp() {
        useCase = GetTripProjectionUseCaseImpl(rentingRepository, historyRepository, logger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createContract(startDate: Long) = RentingContract(
        id = "c1",
        userId = "u1",
        vehicleName = "Car",
        startDate = startDate,
        durationMonths = 12,
        totalKms = 12000.0,
        startOdometer = 10000.0,
        currentOdometer = 10000.0
    )

    @Test
    fun `given no contract when invoke then emits Progress and Success with null projection`() = runTest {
        every { rentingRepository.getContract() } returns flowOf(null)

        val emissions = useCase(GetTripProjectionUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetTripProjectionUseCase.Output.Progress)
        val success = emissions[1] as GetTripProjectionUseCase.Output.Success
        assertNull(success.projection)
    }

    @Test
    fun `given contract started 2 days ago when invoke then hasEnoughData is false`() = runTest {
        val twoDaysMillis = 2 * 1000L * 60 * 60 * 24
        val startDate = System.currentTimeMillis() - twoDaysMillis
        val contract = createContract(startDate)

        every { rentingRepository.getContract() } returns flowOf(contract)
        every { historyRepository.getHistory(contract.id) } returns flowOf(emptyList())

        val emissions = useCase(GetTripProjectionUseCase.Input).toList()

        val success = emissions[1] as GetTripProjectionUseCase.Output.Success
        val projection = success.projection
        assertNotNull(projection)
        assertFalse(projection!!.hasEnoughData)
    }

    @Test
    fun `given contract started 30 days ago and reasonable driving when invoke then hasEnoughData is true and isOverLimit false`() = runTest {
        val thirtyDaysMillis = 30 * 1000L * 60 * 60 * 24
        val startDate = System.currentTimeMillis() - thirtyDaysMillis
        val contract = createContract(startDate)

        val trip = OdometerRecord(
            id = "r1",
            contractId = contract.id,
            timestamp = startDate + 1000,
            odometerValue = 300.0, // 10 km/day average
            isInitialRecord = false
        )

        every { rentingRepository.getContract() } returns flowOf(contract)
        every { historyRepository.getHistory(contract.id) } returns flowOf(listOf(trip))

        val emissions = useCase(GetTripProjectionUseCase.Input).toList()

        val success = emissions[1] as GetTripProjectionUseCase.Output.Success
        val projection = success.projection
        assertNotNull(projection)
        assertTrue(projection!!.hasEnoughData)
        assertFalse(projection.isOverLimit)
        assertTrue(projection.expectedFinalBalance > 0)
    }

    @Test
    fun `given repository error when invoke then catches and emits Failure`() = runTest {
        every { rentingRepository.getContract() } returns flow { throw RuntimeException("Projection error") }

        val emissions = useCase(GetTripProjectionUseCase.Input).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is GetTripProjectionUseCase.Output.Progress)
        val failure = emissions[1] as GetTripProjectionUseCase.Output.Failure
        assertEquals("Projection error", failure.message)
    }
}
