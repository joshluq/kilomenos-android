package es.joshluq.kmsafe.feature.projection

import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.TripProjection
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.GetOverviewDataUseCase
import es.joshluq.kmsafe.domain.usecase.GetRentingContractUseCase
import es.joshluq.kmsafe.domain.usecase.GetTripProjectionUseCase
import es.joshluq.kmsafe.domain.usecase.SimulateContractProjectionUseCase
import es.joshluq.kmsafe.domain.usecase.SimulateContractProjectionUseCaseImpl
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectionAnalysisViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getTripProjectionUseCase: GetTripProjectionUseCase = mockk()
    private val getOverviewDataUseCase: GetOverviewDataUseCase = mockk()
    private val getRentingContractUseCase: GetRentingContractUseCase = mockk()
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase = mockk()
    private val simulateContractProjectionUseCase: SimulateContractProjectionUseCase = SimulateContractProjectionUseCaseImpl()
    private val analytics: AnalyticskitManager = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)

    private val sampleContract = RentingContract(
        id = "contract-proj-1",
        vehicleName = "Volvo XC40",
        startDate = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 30), // 30 days ago
        durationMonths = 12,
        totalKms = 15000.0,
        startOdometer = 10000.0,
        currentOdometer = 11500.0,
        excessDistancePrice = 0.08
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
        every { checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.ADVANCED_PROJECTIONS)) } returns flowOf(
            CheckFeatureAccessUseCase.Output.Success(isGranted = true)
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
            checkFeatureAccessUseCase = checkFeatureAccessUseCase,
            simulateContractProjectionUseCase = simulateContractProjectionUseCase,
            analytics = analytics,
            logger = logger
        )
    }

    @Test
    fun `given projection and contract data when initialized then calculates simulation and recommendation`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.isPremium)
        assertEquals(sampleProjection, viewModel.state.value.baselineProjection)
        assertEquals(45.0f, viewModel.state.value.realDailyAverage, 0.01f)
        assertEquals(45.0f, viewModel.state.value.simulatedDailyKm, 0.01f)
        assertEquals(15000.0, viewModel.state.value.totalContractKms, 0.01)
        assertEquals(0.08f, viewModel.state.value.penaltyPricePerKm, 0.001f)
        assertNotNull(viewModel.state.value.remedialDailyKm)
    }

    @Test
    fun `given pace preset selected then updates multiplier and recalculates simulation`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnPacePresetSelected(1.2f))
        advanceUntilIdle()

        assertEquals(1.2f, viewModel.state.value.paceMultiplier, 0.01f)
        assertEquals(45.0f * 1.2f, viewModel.state.value.simulatedDailyKm, 0.01f)
    }

    @Test
    fun `given planned trips added and removed then updates state accordingly`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnAddPresetTrip("Escapada", 350))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.plannedTrips.size)
        assertEquals(350, viewModel.state.value.totalPlannedTripsKm)

        val tripId = viewModel.state.value.plannedTrips.first().id
        viewModel.sendEvent(Event.OnRemoveTrip(tripId))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.plannedTrips.isEmpty())
        assertEquals(0, viewModel.state.value.totalPlannedTripsKm)
    }

    @Test
    fun `given free user attempts to add multiple trips then emits NavigateToPremiumPaywall`() = runTest(testDispatcher) {
        every { checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.ADVANCED_PROJECTIONS)) } returns flowOf(
            CheckFeatureAccessUseCase.Output.Success(isGranted = false)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isPremium)

        val effects = mutableListOf<Effect>()
        val job = launch(UnconfinedTestDispatcher()) {
            viewModel.effects.toList(effects)
        }

        // Add first trip (allowed for Free trial)
        viewModel.sendEvent(Event.OnAddPresetTrip("Escapada 1", 350))
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.plannedTrips.size)
        assertTrue(effects.isEmpty())

        // Add second trip (blocked for Free)
        viewModel.sendEvent(Event.OnAddPresetTrip("Escapada 2", 500))
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertTrue(effects.first() is Effect.NavigateToPremiumPaywall)

        job.cancel()
    }

    @Test
    fun `given upgrade clicked then emits NavigateToPremiumPaywall`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val effects = mutableListOf<Effect>()
        val job = launch(UnconfinedTestDispatcher()) {
            viewModel.effects.toList(effects)
        }

        viewModel.sendEvent(Event.OnUpgradeToPremiumClicked)
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertTrue(effects.first() is Effect.NavigateToPremiumPaywall)

        job.cancel()
    }
}
