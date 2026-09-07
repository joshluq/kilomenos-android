package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.model.PlannedTrip
import es.joshluq.kmsafe.domain.model.RentingContract
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SimulateContractProjectionUseCaseTest {

    private lateinit var useCase: SimulateContractProjectionUseCase

    companion object {
        private const val MILLIS_IN_DAY = 1000L * 60 * 60 * 24
        private const val DAYS_IN_MONTH = 30.4375
    }

    @Before
    fun setUp() {
        useCase = SimulateContractProjectionUseCaseImpl()
    }

    private fun createContract(
        startDate: Long = 1_000_000_000L,
        durationMonths: Int = 12,
        totalKms: Double = 12_000.0,
        startOdometer: Double = 10_000.0
    ) = RentingContract(
        id = "contract-1",
        userId = "user-1",
        vehicleName = "Test Car",
        startDate = startDate,
        durationMonths = durationMonths,
        totalKms = totalKms,
        startOdometer = startOdometer,
        currentOdometer = startOdometer
    )

    @Test
    fun `given safe pace within budget then balance is positive and penalty is zero`() = runTest {
        val startDate = 1_000_000_000L
        val contract = createContract(startDate = startDate, durationMonths = 12, totalKms = 12000.0)
        // 12 months = 365.25 days. Total daily budget ~ 32.85 km/day.
        // If driver simulates 20 km/day, they should have surplus.
        val currentTime = startDate + (30 * MILLIS_IN_DAY) // 30 days in

        val result = useCase(
            SimulateContractProjectionUseCase.Input(
                contract = contract,
                actualKmsDrivenSinceStart = 600.0, // 20 km/day so far
                simulatedDailyKm = 20.0f,
                penaltyPricePerKm = 0.08f,
                currentTime = currentTime
            )
        )

        val output = result.getOrThrow() as SimulateContractProjectionUseCase.Output.Success
        val sim = output.result

        assertFalse(sim.isOverLimit)
        assertEquals(0.0, sim.estimatedPenalty, 0.001)
        assertTrue(sim.simulatedFinalBalance > 0.0)
        assertNull(sim.exhaustionDateMillis)
        assertEquals(0, sim.monthsAheadOrBehind)
        assertNotNull(sim.remedialDailyKm)
        assertTrue(sim.remedialDailyKm!! > 20.0)
    }

    @Test
    fun `given excess pace then calculates balance, penalty, exhaustion date and remedial pace`() = runTest {
        val startDate = 1_000_000_000L
        val contract = createContract(startDate = startDate, durationMonths = 12, totalKms = 12000.0)
        val currentTime = startDate + (60 * MILLIS_IN_DAY) // 60 days in

        // Total contract days = 365.25. Remaining days ~ 305.25.
        // If driver drives 50 km/day: 50 * 305.25 = 15262.5 km + 3000 km driven = 18262.5 km vs 12000 limit.
        val result = useCase(
            SimulateContractProjectionUseCase.Input(
                contract = contract,
                actualKmsDrivenSinceStart = 3000.0,
                simulatedDailyKm = 50.0f,
                penaltyPricePerKm = 0.10f,
                currentTime = currentTime
            )
        )

        val output = result.getOrThrow() as SimulateContractProjectionUseCase.Output.Success
        val sim = output.result

        assertTrue(sim.isOverLimit)
        assertTrue(sim.simulatedFinalBalance < 0.0)
        assertTrue(sim.estimatedPenalty > 0.0)
        assertEquals(-sim.simulatedFinalBalance * 0.10, sim.estimatedPenalty, 0.01)

        // Exhaustion date should exist and be before contract end date
        assertNotNull(sim.exhaustionDateMillis)
        assertTrue(sim.monthsAheadOrBehind > 0)

        // Remedial pace to avoid penalty should be available and less than 50 km/day
        assertNotNull(sim.remedialDailyKm)
        assertTrue(sim.remedialDailyKm!! < 50.0)
    }

    @Test
    fun `given planned trips stacked then additional kms reduce balance and increase penalty`() = runTest {
        val startDate = 1_000_000_000L
        val contract = createContract(startDate = startDate, durationMonths = 12, totalKms = 12000.0)
        val currentTime = startDate + (60 * MILLIS_IN_DAY)

        val baselineResult = useCase(
            SimulateContractProjectionUseCase.Input(
                contract = contract,
                actualKmsDrivenSinceStart = 2000.0,
                simulatedDailyKm = 30.0f,
                plannedTrips = emptyList(),
                penaltyPricePerKm = 0.05f,
                currentTime = currentTime
            )
        ).getOrThrow() as SimulateContractProjectionUseCase.Output.Success

        val stackedTrips = listOf(
            PlannedTrip(title = "Semana Santa", distanceKms = 500),
            PlannedTrip(title = "Vacaciones Verano", distanceKms = 1500)
        )

        val withTripsResult = useCase(
            SimulateContractProjectionUseCase.Input(
                contract = contract,
                actualKmsDrivenSinceStart = 2000.0,
                simulatedDailyKm = 30.0f,
                plannedTrips = stackedTrips,
                penaltyPricePerKm = 0.05f,
                currentTime = currentTime
            )
        ).getOrThrow() as SimulateContractProjectionUseCase.Output.Success

        assertEquals(
            baselineResult.result.simulatedProjectedTotalKms + 2000.0,
            withTripsResult.result.simulatedProjectedTotalKms,
            0.01
        )
        assertEquals(
            baselineResult.result.simulatedFinalBalance - 2000.0,
            withTripsResult.result.simulatedFinalBalance,
            0.01
        )
    }

    @Test
    fun `given contract already exceeded allowance then remedial pace is zero`() = runTest {
        val startDate = 1_000_000_000L
        val contract = createContract(startDate = startDate, durationMonths = 12, totalKms = 10000.0)
        val currentTime = startDate + (60 * MILLIS_IN_DAY)

        val result = useCase(
            SimulateContractProjectionUseCase.Input(
                contract = contract,
                actualKmsDrivenSinceStart = 11000.0, // already exceeded
                simulatedDailyKm = 20.0f,
                currentTime = currentTime
            )
        )

        val output = result.getOrThrow() as SimulateContractProjectionUseCase.Output.Success
        assertEquals(0.0, output.result.remedialDailyKm!!, 0.001)
    }

    @Test
    fun `given contract expired then remedial pace is null`() = runTest {
        val startDate = 1_000_000_000L
        val contract = createContract(startDate = startDate, durationMonths = 12, totalKms = 10000.0)
        val currentTime = startDate + (400 * MILLIS_IN_DAY) // expired

        val result = useCase(
            SimulateContractProjectionUseCase.Input(
                contract = contract,
                actualKmsDrivenSinceStart = 5000.0,
                simulatedDailyKm = 20.0f,
                currentTime = currentTime
            )
        )

        val output = result.getOrThrow() as SimulateContractProjectionUseCase.Output.Success
        assertNull(output.result.remedialDailyKm)
    }
}
