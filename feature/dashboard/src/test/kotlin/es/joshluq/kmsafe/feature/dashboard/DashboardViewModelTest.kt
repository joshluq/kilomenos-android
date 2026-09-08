package es.joshluq.kmsafe.feature.dashboard

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.usecase.GetCurrentUserUseCase
import es.joshluq.kmsafe.domain.usecase.GetRentingContractUseCase
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
    private val logger: LoggerKit = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getCurrentUserUseCase(any()) } returns flowOf(GetCurrentUserUseCase.Output.Success(null))
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

        // User logs out (user becomes null)
        userFlow.emit(GetCurrentUserUseCase.Output.Success(null))
        advanceUntilIdle()
        assertEquals(DashboardTab.OVERVIEW, viewModel.state.value.selectedTab)

        // User logs back in as user1 again
        viewModel.sendEvent(Event.OnTabSelected(DashboardTab.PROFILE))
        advanceUntilIdle()
        assertEquals(DashboardTab.PROFILE, viewModel.state.value.selectedTab)

        userFlow.emit(GetCurrentUserUseCase.Output.Success(User(id = "user1", email = "u1@test.com", name = "User 1")))
        advanceUntilIdle()
        assertEquals(DashboardTab.OVERVIEW, viewModel.state.value.selectedTab)
    }
}

