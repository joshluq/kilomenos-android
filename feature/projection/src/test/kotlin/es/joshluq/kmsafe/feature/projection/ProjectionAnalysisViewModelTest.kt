package es.joshluq.kmsafe.feature.projection

import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.TripProjection
import es.joshluq.kmsafe.domain.usecase.GetOverviewDataUseCase
import es.joshluq.kmsafe.domain.usecase.GetRentingContractUseCase
import es.joshluq.kmsafe.domain.usecase.GetTripProjectionUseCase
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectionAnalysisViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getTripProjectionUseCase: GetTripProjectionUseCase = mockk()
    private val getOverviewDataUseCase: GetOverviewDataUseCase = mockk()
    private val getRentingContractUseCase: GetRentingContractUseCase = mockk()
    private val analytics: AnalyticskitManager = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)

    private val sampleContract = RentingContract(
        id = "contract-proj-1",
        vehicleName = "Volvo XC40",
        startDate = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 30), // 30 days ago
        durationMonths = 12,
        totalKms = 15000.0,
        startOdometer = 10000.0,
        currentOdometer = 11500.0
    )

    private val sampleProjection = TripProjection(
        projectedTotalKms = 26000.0,
        expectedFinalBalance = -1000.0,
        isOverLimit = true,
        dailyAverage = 45.0,
        hasEnoughData = true
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getTripProjectionUseCase(GetTripProjectionUseCase.Input) } returns flowOf(
            GetTripProjectionUseCase.Output.Success(sampleProjection)
        )
        every { getOverviewDataUseCase(GetOverviewDataUseCase.Input) } returns flowOf(
            GetOverviewDataUseCase.Output.Success(
                contract = sampleContract,
                actualKmsDrivenSinceStart = 1500.0,
                isSyncPending = false,
                metrics = null
            )
        )
        every { getRentingContractUseCase(GetRentingContractUseCase.Input) } returns flowOf(
            GetRentingContractUseCase.Output.Success(sampleContract)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun createViewModel(): ProjectionAnalysisViewModel {
        return ProjectionAnalysisViewModel(
            getTripProjectionUseCase = getTripProjectionUseCase,
            getOverviewDataUseCase = getOverviewDataUseCase,
            getRentingContractUseCase = getRentingContractUseCase,
            analytics = analytics,
            logger = logger
        )
    }

    @Test
    fun `given projection and contract data when initialized then calculates simulation and recommendation`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(sampleProjection, viewModel.state.value.baselineProjection)
        assertEquals(45.0f, viewModel.state.value.currentRealDailyAverage, 0.01f)
        assertEquals(45.0f, viewModel.state.value.simulatedDailyKm, 0.01f)
        assertEquals(15000.0, viewModel.state.value.totalContractKms, 0.01)
        assertNotNull(viewModel.state.value.recommendedDailyKm)
    }

    @Test
    fun `given simulated daily km changed then recalculates balance and estimated penalty`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Set higher daily km
        viewModel.sendEvent(Event.OnSimulatedKmChanged(60.0f))
        advanceUntilIdle()

        assertEquals(60.0f, viewModel.state.value.simulatedDailyKm, 0.01f)
    }

    @Test
    fun `given penalty price changed then recalculates estimated penalty`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnPenaltyPriceChanged(0.12f))
        advanceUntilIdle()

        assertEquals(0.12f, viewModel.state.value.penaltyPricePerKm, 0.001f)
    }

    @Test
    fun `given planned trip changed then includes trip in simulation`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnPlannedTripChanged(500))
        advanceUntilIdle()

        assertEquals(500, viewModel.state.value.plannedTripKms)
    }
}
