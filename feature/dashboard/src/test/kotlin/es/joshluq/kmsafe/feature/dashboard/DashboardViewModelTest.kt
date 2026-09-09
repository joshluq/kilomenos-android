package es.joshluq.kmsafe.feature.dashboard

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.AppOverlayState
import es.joshluq.kmsafe.domain.model.FleetSwitchingState
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.usecase.GetCurrentUserUseCase
import es.joshluq.kmsafe.domain.usecase.GetRentingContractUseCase
import es.joshluq.kmsafe.domain.usecase.ObserveAppOverlayUseCase
import es.joshluq.kmsafe.domain.usecase.ObserveFleetSwitchingUseCase
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getRentingContractUseCase: GetRentingContractUseCase = mockk(relaxed = true)
    private val getCurrentUserUseCase: GetCurrentUserUseCase = mockk(relaxed = true)
    private val observeFleetSwitchingUseCase: ObserveFleetSwitchingUseCase = mockk(relaxed = true)
    private val observeAppOverlayUseCase: ObserveAppOverlayUseCase = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getCurrentUserUseCase(any()) } returns flowOf(GetCurrentUserUseCase.Output.Success(null))
        every { observeFleetSwitchingUseCase(any()) } returns flowOf(
            ObserveFleetSwitchingUseCase.Output.Success(FleetSwitchingState(isSwitching = false, vehicleName = null))
        )
        every { observeAppOverlayUseCase(any()) } returns flowOf(
            ObserveAppOverlayUseCase.Output.Success(AppOverlayState.None)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun createViewModel(): DashboardViewModel {
        return DashboardViewModel(
            getRentingContractUseCase = getRentingContractUseCase,
            getCurrentUserUseCase = getCurrentUserUseCase,
            observeFleetSwitchingUseCase = observeFleetSwitchingUseCase,
            observeAppOverlayUseCase = observeAppOverlayUseCase,
            logger = logger
        )
    }

    @Test
    fun `given active renting contract when initialized then updates hasRentingContract to true`() = runTest(testDispatcher) {
        val testContract = RentingContract(
            id = "c1",
            vehicleName = "Golf 8",
            startDate = 1000L,
            durationMonths = 36,
            totalKms = 15000.0,
            startOdometer = 10000.0,
            currentOdometer = 12000.0
        )
        every { getRentingContractUseCase(GetRentingContractUseCase.Input) } returns flowOf(
            GetRentingContractUseCase.Output.Success(testContract)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.hasRentingContract)
    }

    @Test
    fun `given no renting contract when initialized then sets hasRentingContract to false`() = runTest(testDispatcher) {
        every { getRentingContractUseCase(GetRentingContractUseCase.Input) } returns flowOf(
            GetRentingContractUseCase.Output.Failure
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.hasRentingContract)
    }

    @Test
    fun `given tab selected event when different tab then updates state and emits NavigateToTab effect`() = runTest(testDispatcher) {
        every { getRentingContractUseCase(GetRentingContractUseCase.Input) } returns flowOf(
            GetRentingContractUseCase.Output.Failure
        )

        val effects = mutableListOf<Effect>()
        val viewModel = createViewModel()
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnTabSelected(DashboardTab.EXPENSES))
        advanceUntilIdle()

        assertEquals(DashboardTab.EXPENSES, viewModel.state.value.selectedTab)
        assertEquals(1, effects.size)
        assertEquals(Effect.NavigateToTab(DashboardTab.EXPENSES), effects.first())
    }

    @Test
    fun `given odometer click event then opens dialog in state`() = runTest(testDispatcher) {
        every { getRentingContractUseCase(GetRentingContractUseCase.Input) } returns flowOf(
            GetRentingContractUseCase.Output.Failure
        )

        val viewModel = createViewModel()
        assertFalse(viewModel.state.value.showUpdateDialog)

        viewModel.sendEvent(Event.OnOdometerClicked)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.showUpdateDialog)
        assertEquals("", viewModel.state.value.currentMileageInput)
    }

    @Test
    fun `given dismiss odometer dialog event then closes dialog`() = runTest(testDispatcher) {
        every { getRentingContractUseCase(GetRentingContractUseCase.Input) } returns flowOf(
            GetRentingContractUseCase.Output.Failure
        )

        val viewModel = createViewModel()
        viewModel.sendEvent(Event.OnOdometerClicked)
        assertTrue(viewModel.state.value.showUpdateDialog)

        viewModel.sendEvent(Event.OnDismissOdometerDialog)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.showUpdateDialog)
    }

    @Test
    fun `given ResetToOverview event then resets selectedTab to OVERVIEW`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.sendEvent(Event.OnTabSelected(DashboardTab.PROFILE))
        advanceUntilIdle()
        assertEquals(DashboardTab.PROFILE, viewModel.state.value.selectedTab)

        viewModel.sendEvent(Event.ResetToOverview)
        advanceUntilIdle()

        assertEquals(DashboardTab.OVERVIEW, viewModel.state.value.selectedTab)
    }

    @Test
    fun `given user session change then automatically resets selectedTab to OVERVIEW`() = runTest(testDispatcher) {
        val userFlow = MutableSharedFlow<GetCurrentUserUseCase.Output>()
        every { getCurrentUserUseCase(any()) } returns userFlow

        val viewModel = createViewModel()
        advanceUntilIdle()

        // User logs in as user1
        userFlow.emit(GetCurrentUserUseCase.Output.Success(User(id = "user1", email = "u1@test.com", name = "User 1")))
        advanceUntilIdle()
        assertEquals(DashboardTab.OVERVIEW, viewModel.state.value.selectedTab)

        // User navigates to PROFILE
        viewModel.sendEvent(Event.OnTabSelected(DashboardTab.PROFILE))
        advanceUntilIdle()
        assertEquals(DashboardTab.PROFILE, viewModel.state.value.selectedTab)

        // User logs out (user becomes null) - tab must NOT be flipped to OVERVIEW so logout navigation can complete
        userFlow.emit(GetCurrentUserUseCase.Output.Success(null))
        advanceUntilIdle()
        assertEquals(DashboardTab.PROFILE, viewModel.state.value.selectedTab)

        // User logs back in as user1 again
        viewModel.sendEvent(Event.OnTabSelected(DashboardTab.PROFILE))
        advanceUntilIdle()
        assertEquals(DashboardTab.PROFILE, viewModel.state.value.selectedTab)

        userFlow.emit(GetCurrentUserUseCase.Output.Success(User(id = "user1", email = "u1@test.com", name = "User 1")))
        advanceUntilIdle()
        assertEquals(DashboardTab.OVERVIEW, viewModel.state.value.selectedTab)
    }

    @Test
    fun `given fleet switching flow when switching state changes then updates state`() = runTest(testDispatcher) {
        val switchingFlow = MutableSharedFlow<ObserveFleetSwitchingUseCase.Output>(replay = 1)
        every { observeFleetSwitchingUseCase(any()) } returns switchingFlow

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSwitchingVehicle)
        assertEquals(null, viewModel.state.value.switchingVehicleName)

        switchingFlow.emit(
            ObserveFleetSwitchingUseCase.Output.Success(
                FleetSwitchingState(isSwitching = true, vehicleName = "Peugeot 3008")
            )
        )
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isSwitchingVehicle)
        assertEquals("Peugeot 3008", viewModel.state.value.switchingVehicleName)

        switchingFlow.emit(
            ObserveFleetSwitchingUseCase.Output.Success(
                FleetSwitchingState(isSwitching = false, vehicleName = null)
            )
        )
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSwitchingVehicle)
        assertEquals(null, viewModel.state.value.switchingVehicleName)
    }

    @Test
    fun `given vehicle switching active when tab selected then ignores navigation`() = runTest(testDispatcher) {
        val switchingFlow = MutableSharedFlow<ObserveFleetSwitchingUseCase.Output>(replay = 1)
        every { observeFleetSwitchingUseCase(any()) } returns switchingFlow

        val effects = mutableListOf<Effect>()
        val viewModel = createViewModel()
        advanceUntilIdle()

        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        switchingFlow.emit(
            ObserveFleetSwitchingUseCase.Output.Success(
                FleetSwitchingState(isSwitching = true, vehicleName = "Peugeot 3008")
            )
        )
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnTabSelected(DashboardTab.EXPENSES))
        advanceUntilIdle()

        assertEquals(DashboardTab.OVERVIEW, viewModel.state.value.selectedTab)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `given hud overlay active when observed then state reflects overlay and blocks navigation`() = runTest(testDispatcher) {
        val overlayFlow = MutableSharedFlow<ObserveAppOverlayUseCase.Output>(replay = 1)
        every { observeAppOverlayUseCase(any()) } returns overlayFlow

        val effects = mutableListOf<Effect>()
        val viewModel = createViewModel()
        advanceUntilIdle()

        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        overlayFlow.emit(ObserveAppOverlayUseCase.Output.Success(AppOverlayState.LoggingOut))
        advanceUntilIdle()

        assertEquals(AppOverlayState.LoggingOut, viewModel.state.value.hudOverlayState)
        assertTrue(viewModel.state.value.isNavigationBlocked)

        viewModel.sendEvent(Event.OnTabSelected(DashboardTab.PROFILE))
        advanceUntilIdle()

        assertEquals(DashboardTab.OVERVIEW, viewModel.state.value.selectedTab)
        assertTrue(effects.isEmpty())

        overlayFlow.emit(ObserveAppOverlayUseCase.Output.Success(AppOverlayState.None))
        advanceUntilIdle()

        assertEquals(AppOverlayState.None, viewModel.state.value.hudOverlayState)
        assertFalse(viewModel.state.value.isNavigationBlocked)
    }
}
