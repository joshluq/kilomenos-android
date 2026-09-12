package es.joshluq.kmsafe.feature.fleet.vehicles

import es.joshluq.kmsafe.core.analytics.fake.FakeAnalyticsTracker
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteContractUseCase
import es.joshluq.kmsafe.domain.usecase.GetAllContractsUseCase
import es.joshluq.kmsafe.domain.usecase.SelectContractUseCase
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getAllContractsUseCase: GetAllContractsUseCase = mockk()
    private val deleteContractUseCase: DeleteContractUseCase = mockk()
    private val selectContractUseCase: SelectContractUseCase = mockk()
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase = mockk()
    private val analytics = FakeAnalyticsTracker()
    private val logger: LoggerKit = mockk(relaxed = true)

    private val vehicle1 = RentingContract(
        id = "v1",
        vehicleName = "Car 1",
        startDate = 1000L,
        durationMonths = 12,
        totalKms = 10000.0,
        startOdometer = 5000.0,
        currentOdometer = 6000.0,
        isSelected = true
    )

    private val vehicle2 = RentingContract(
        id = "v2",
        vehicleName = "Car 2",
        startDate = 2000L,
        durationMonths = 24,
        totalKms = 20000.0,
        startOdometer = 1000.0,
        currentOdometer = 3000.0,
        isSelected = false
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.MULTI_VEHICLE)) } returns flowOf(
            CheckFeatureAccessUseCase.Output.Success(isGranted = true)
        )
        every { getAllContractsUseCase(GetAllContractsUseCase.Input) } returns flowOf(
            GetAllContractsUseCase.Output.Success(listOf(vehicle1, vehicle2))
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun createViewModel(): VehicleListViewModel {
        return VehicleListViewModel(
            getAllContractsUseCase = getAllContractsUseCase,
            deleteContractUseCase = deleteContractUseCase,
            selectContractUseCase = selectContractUseCase,
            checkFeatureAccessUseCase = checkFeatureAccessUseCase,
            analytics = analytics,
            logger = logger
        )
    }

    @Test
    fun `given contracts loaded when initialized then state contains vehicles and premium status`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.isPremium)
        assertEquals(2, viewModel.state.value.vehicles.size)
    }

    @Test
    fun `given delete selected vehicle requested then shows error in state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnDeleteVehicleClicked(vehicle1))
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
        assertNull(viewModel.state.value.vehicleToDelete)
    }

    @Test
    fun `given delete unselected vehicle requested then sets vehicleToDelete in state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnDeleteVehicleClicked(vehicle2))
        advanceUntilIdle()

        assertEquals(vehicle2, viewModel.state.value.vehicleToDelete)
    }

    @Test
    fun `given delete confirmed when success then deletes vehicle and refreshes list`() = runTest(testDispatcher) {
        every { deleteContractUseCase(DeleteContractUseCase.Input("v2")) } returns flowOf(
            DeleteContractUseCase.Output.Success
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnDeleteVehicleClicked(vehicle2))
        viewModel.sendEvent(Event.OnDeleteConfirmed)
        advanceUntilIdle()

        assertNull(viewModel.state.value.vehicleToDelete)
        coVerify { deleteContractUseCase(DeleteContractUseCase.Input("v2")) }
    }

    @Test
    fun `given delete cancelled then clears vehicleToDelete in state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnDeleteVehicleClicked(vehicle2))
        assertEquals(vehicle2, viewModel.state.value.vehicleToDelete)

        viewModel.sendEvent(Event.OnDeleteCancelled)
        assertNull(viewModel.state.value.vehicleToDelete)
    }

    @Test
    fun `given vehicle selected event then triggers selectContractUseCase`() = runTest(testDispatcher) {
        every { selectContractUseCase(SelectContractUseCase.Input("v2")) } returns flowOf(
            SelectContractUseCase.Output.Success
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnVehicleSelected("v2"))
        advanceUntilIdle()

        coVerify { selectContractUseCase(SelectContractUseCase.Input("v2")) }
    }

    @Test
    fun `given add vehicle clicked when premium then emits NavigateToAddVehicle effect`() = runTest(testDispatcher) {
        val effects = mutableListOf<Effect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnAddVehicleClicked)
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(Effect.NavigateToAddVehicle, effects.first())
    }

    @Test
    fun `given add vehicle clicked when free and has vehicles then shows premium limit dialog`() = runTest(testDispatcher) {
        every { checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.MULTI_VEHICLE)) } returns flowOf(
            CheckFeatureAccessUseCase.Output.Success(isGranted = false)
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnAddVehicleClicked)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.showPremiumLimit)
    }

    @Test
    fun `given vehicle details clicked then emits NavigateToVehicleDetails effect`() = runTest(testDispatcher) {
        val effects = mutableListOf<Effect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnVehicleDetailsClicked("v1"))
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(Effect.NavigateToVehicleDetails("v1"), effects.first())
    }

    @Test
    fun `given upgrade clicked then emits NavigateToPremiumPaywall effect`() = runTest(testDispatcher) {
        val effects = mutableListOf<Effect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnUpgradeClicked)
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(Effect.NavigateToPremiumPaywall, effects.first())
    }
}
