package es.joshluq.kmsafe.feature.overview

import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.core.domain.usecase.SyncStationGeofencesUseCase
import es.joshluq.kmsafe.core.monetization.domain.MonetizationConfig
import es.joshluq.kmsafe.domain.model.ContractMetrics
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.UserPreferences
import es.joshluq.kmsafe.domain.usecase.AddOdometerRecordUseCase
import es.joshluq.kmsafe.domain.usecase.ClearTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.GetAllContractsUseCase
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.GetMonthlyUsageUseCase
import es.joshluq.kmsafe.domain.usecase.GetOverviewDataUseCase
import es.joshluq.kmsafe.domain.usecase.GetPreferencesUseCase
import es.joshluq.kmsafe.domain.usecase.GetTripProjectionUseCase
import es.joshluq.kmsafe.domain.usecase.ObserveTrackingStateUseCase
import es.joshluq.kmsafe.domain.usecase.ObserveVehicleBluetoothConnectionUseCase
import es.joshluq.kmsafe.domain.usecase.SelectContractUseCase
import es.joshluq.kmsafe.domain.usecase.StartAutoTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StartTripTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StopAutoTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StopTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StopTripTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.UpdatePreferencesUseCase
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OverviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getOverviewDataUseCase: GetOverviewDataUseCase = mockk(relaxed = true)
    private val getMonthlyUsageUseCase: GetMonthlyUsageUseCase = mockk(relaxed = true)
    private val addOdometerRecordUseCase: AddOdometerRecordUseCase = mockk(relaxed = true)
    private val getTripProjectionUseCase: GetTripProjectionUseCase = mockk(relaxed = true)
    private val getAllContractsUseCase: GetAllContractsUseCase = mockk(relaxed = true)
    private val selectContractUseCase: SelectContractUseCase = mockk(relaxed = true)
    private val getEntitlementsUseCase: GetEntitlementsUseCase = mockk(relaxed = true)
    private val getPreferencesUseCase: GetPreferencesUseCase = mockk(relaxed = true)
    private val updatePreferencesUseCase: UpdatePreferencesUseCase = mockk(relaxed = true)
    private val observeTrackingStateUseCase: ObserveTrackingStateUseCase = mockk(relaxed = true)
    private val observeVehicleBluetoothConnectionUseCase: ObserveVehicleBluetoothConnectionUseCase = mockk(relaxed = true)
    private val stopTrackingUseCase: StopTrackingUseCase = mockk(relaxed = true)
    private val clearTrackingUseCase: ClearTrackingUseCase = mockk(relaxed = true)
    private val startAutoTrackingUseCase: StartAutoTrackingUseCase = mockk(relaxed = true)
    private val stopAutoTrackingUseCase: StopAutoTrackingUseCase = mockk(relaxed = true)
    private val startTripTrackingUseCase: StartTripTrackingUseCase = mockk(relaxed = true)
    private val stopTripTrackingUseCase: StopTripTrackingUseCase = mockk(relaxed = true)
    private val syncStationGeofencesUseCase: SyncStationGeofencesUseCase = mockk(relaxed = true)
    private val monetizationConfig: MonetizationConfig = mockk(relaxed = true)
    private val analytics: AnalyticskitManager = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)

    private val sampleContract = RentingContract(
        id = "contract-1",
        vehicleName = "Audi A3",
        startDate = System.currentTimeMillis() - 10000000L,
        durationMonths = 12,
        totalKms = 15000.0,
        startOdometer = 10000.0,
        currentOdometer = 12500.0,
        bluetoothDeviceAddress = "00:11:22:33:44:55"
    )

    private val sampleMetrics = ContractMetrics(
        contract = sampleContract,
        actualKmsDriven = 2500.0,
        currentOdometer = 12500.0,
        theoreticalKms = 2650.0,
        balance = 150.0,
        dailyBudget = 41.0,
        monthlyBudget = 1250.0,
        timePercentage = 25f,
        kmsPercentage = 20f,
        differencePercentage = -5f,
        isSyncPending = false
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { monetizationConfig.getBannerAdUnitId() } returns "test_ad_unit_id"
        every { getEntitlementsUseCase(any()) } returns flowOf(
            GetEntitlementsUseCase.Output.Success(Entitlements.Default.copy(subscriptionLevel = SubscriptionLevel.PREMIUM))
        )
        every { getPreferencesUseCase(any()) } returns flowOf(
            GetPreferencesUseCase.Output.Success(UserPreferences())
        )
        every { getOverviewDataUseCase(any()) } returns flowOf(
            GetOverviewDataUseCase.Output.Success(
                contract = sampleContract,
                actualKmsDrivenSinceStart = 2500.0,
                isSyncPending = false,
                metrics = sampleMetrics
            )
        )
        every { getAllContractsUseCase(any()) } returns flowOf(
            GetAllContractsUseCase.Output.Success(listOf(sampleContract))
        )
        every { getMonthlyUsageUseCase(any()) } returns flowOf(
            GetMonthlyUsageUseCase.Output.Success(emptyList())
        )
        every { getTripProjectionUseCase(any()) } returns flowOf(
            GetTripProjectionUseCase.Output.Success(null)
        )
        every { observeTrackingStateUseCase(any()) } returns flowOf(
            ObserveTrackingStateUseCase.Output.Success(
                isTracking = false,
                trackedDistance = 0.0,
                startTime = null,
                encodedPolyline = null,
                pointCount = 0
            )
        )
        every { syncStationGeofencesUseCase(any()) } returns flowOf(
            SyncStationGeofencesUseCase.Output.Success
        )
        every { updatePreferencesUseCase(any()) } returns flowOf(
            UpdatePreferencesUseCase.Output.Success
        )
        every { clearTrackingUseCase(any()) } returns flowOf(
            ClearTrackingUseCase.Output.Success
        )
        every { stopTrackingUseCase(any()) } returns flowOf(
            StopTrackingUseCase.Output.Success
        )
        every { startAutoTrackingUseCase(any()) } returns flowOf(
            StartAutoTrackingUseCase.Output.Success
        )
        every { stopAutoTrackingUseCase(any()) } returns flowOf(
            StopAutoTrackingUseCase.Output.Success
        )
        every { observeVehicleBluetoothConnectionUseCase(any()) } returns flowOf(
            ObserveVehicleBluetoothConnectionUseCase.Output.Success(isConnected = false)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun createViewModel(): OverviewViewModel {
        return OverviewViewModel(
            getOverviewDataUseCase = getOverviewDataUseCase,
            getMonthlyUsageUseCase = getMonthlyUsageUseCase,
            addOdometerRecordUseCase = addOdometerRecordUseCase,
            getTripProjectionUseCase = getTripProjectionUseCase,
            getAllContractsUseCase = getAllContractsUseCase,
            selectContractUseCase = selectContractUseCase,
            getEntitlementsUseCase = getEntitlementsUseCase,
            getPreferencesUseCase = getPreferencesUseCase,
            updatePreferencesUseCase = updatePreferencesUseCase,
            observeTrackingStateUseCase = observeTrackingStateUseCase,
            observeVehicleBluetoothConnectionUseCase = observeVehicleBluetoothConnectionUseCase,
            stopTrackingUseCase = stopTrackingUseCase,
            clearTrackingUseCase = clearTrackingUseCase,
            startAutoTrackingUseCase = startAutoTrackingUseCase,
            stopAutoTrackingUseCase = stopAutoTrackingUseCase,
            startTripTrackingUseCase = startTripTrackingUseCase,
            stopTripTrackingUseCase = stopTripTrackingUseCase,
            syncStationGeofencesUseCase = syncStationGeofencesUseCase,
            monetizationConfig = monetizationConfig,
            analytics = analytics,
            logger = logger
        )
    }

    @Test
    fun `given valid data when initialized then populates state with contract and metrics`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(sampleContract, state.renting)
        assertEquals(150.0, state.balance, 0.01)
        assertEquals(12500.0, state.totalKmsDriven, 0.01)
        assertEquals("test_ad_unit_id", state.adUnitId)
        assertTrue(state.isPremium == true)
    }

    @Test
    fun `given register renting clicked event then emits NavigateToOnboarding effect`() = runTest(testDispatcher) {
        val effects = mutableListOf<Effect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnRegisterRentingClicked)
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(Effect.NavigateToOnboarding(), effects.first())
    }

    @Test
    fun `given vehicle detail clicked event then emits NavigateToVehicleDetail effect`() = runTest(testDispatcher) {
        val effects = mutableListOf<Effect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnVehicleDetailClicked("contract-1"))
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(Effect.NavigateToVehicleDetail("contract-1"), effects.first())
    }

    @Test
    fun `given update odometer clicked event then opens bottom sheet in state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnUpdateOdometerClicked)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.showBottomSheet)
        assertEquals("", viewModel.state.value.newOdometerValue)
    }

    @Test
    fun `given save record clicked with valid data when saved then closes bottom sheet and clears tracking`() = runTest(testDispatcher) {
        every { addOdometerRecordUseCase(any()) } returns flowOf(
            AddOdometerRecordUseCase.Output.Success
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnUpdateOdometerClicked)
        viewModel.sendEvent(Event.OnNewOdometerChanged("13000"))
        viewModel.sendEvent(Event.OnNewLabelChanged("Daily Commute"))
        viewModel.sendEvent(Event.OnNewFuelChanged("45.5"))
        viewModel.sendEvent(Event.OnSaveRecordClicked(System.currentTimeMillis()))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSaving)
        assertFalse(viewModel.state.value.showBottomSheet)
        coVerify { clearTrackingUseCase(ClearTrackingUseCase.Input) }
    }

    @Test
    fun `given save record clicked with failure then shows dynamic error in state`() = runTest(testDispatcher) {
        every { addOdometerRecordUseCase(any()) } returns flowOf(
            AddOdometerRecordUseCase.Output.Failure("Validation failed")
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnUpdateOdometerClicked)
        viewModel.sendEvent(Event.OnNewOdometerChanged("13000"))
        viewModel.sendEvent(Event.OnSaveRecordClicked(System.currentTimeMillis()))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSaving)
        assertNotNull(viewModel.state.value.error)
    }

    @Test
    fun `given toggle vehicle switcher event then flips showVehicleSwitcher`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.showVehicleSwitcher)

        viewModel.sendEvent(Event.OnToggleVehicleSwitcher)
        assertTrue(viewModel.state.value.showVehicleSwitcher)

        viewModel.sendEvent(Event.OnToggleVehicleSwitcher)
        assertFalse(viewModel.state.value.showVehicleSwitcher)
    }

    @Test
    fun `given switch vehicle clicked when success then closes switcher`() = runTest(testDispatcher) {
        every { selectContractUseCase(SelectContractUseCase.Input("c2")) } returns flowOf(
            SelectContractUseCase.Output.Success
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnToggleVehicleSwitcher)
        assertTrue(viewModel.state.value.showVehicleSwitcher)

        viewModel.sendEvent(Event.OnSwitchVehicleClicked("c2"))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.showVehicleSwitcher)
    }

    @Test
    fun `given start tracking clicked then invokes startTripTrackingUseCase`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.sendEvent(Event.OnStartTrackingClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { startTripTrackingUseCase(StartTripTrackingUseCase.Input) }
    }

    @Test
    fun `given stop tracking clicked then invokes stopTripTrackingUseCase`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.sendEvent(Event.OnStopTrackingClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { stopTripTrackingUseCase(StopTripTrackingUseCase.Input) }
    }

    @Test
    fun `given premium upgrade clicked then emits NavigateToPremiumPaywall effect`() = runTest(testDispatcher) {
        val effects = mutableListOf<Effect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnPremiumUpgradeClicked)
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(Effect.NavigateToPremiumPaywall, effects.first())
    }

    @Test
    fun `given bluetooth connection stream updates then updates state reactively`() = runTest(testDispatcher) {
        val bluetoothFlow = MutableSharedFlow<ObserveVehicleBluetoothConnectionUseCase.Output>()
        every { observeVehicleBluetoothConnectionUseCase(any()) } returns bluetoothFlow

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isVehicleBluetoothConnected)

        bluetoothFlow.emit(ObserveVehicleBluetoothConnectionUseCase.Output.Success(isConnected = true))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isVehicleBluetoothConnected)

        bluetoothFlow.emit(ObserveVehicleBluetoothConnectionUseCase.Output.Success(isConnected = false))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isVehicleBluetoothConnected)
    }
}
