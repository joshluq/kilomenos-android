package es.joshluq.kmsafe.feature.overview

import es.joshluq.kmsafe.core.analytics.fake.FakeAnalyticsTracker
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.core.domain.usecase.SyncStationGeofencesUseCase
import es.joshluq.kmsafe.core.monetization.domain.MonetizationConfig
import es.joshluq.kmsafe.domain.model.ContractMetrics
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.TripProjection
import es.joshluq.kmsafe.domain.model.UserPreferences
import es.joshluq.kmsafe.feature.overview.model.StatusCapsuleUiModel
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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import es.joshluq.kmsafe.domain.model.NotificationPriority
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.domain.usecase.PublishNotificationIfUnreadUseCase
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
    private val observeActiveNotificationsUseCase: es.joshluq.kmsafe.domain.usecase.ObserveActiveNotificationsUseCase = mockk(relaxed = true)
    private val markNotificationAsReadUseCase: es.joshluq.kmsafe.domain.usecase.MarkNotificationAsReadUseCase = mockk(relaxed = true)
    private val publishNotificationIfUnreadUseCase: PublishNotificationIfUnreadUseCase = mockk(relaxed = true)
    private val monetizationConfig: MonetizationConfig = mockk(relaxed = true)
    private val analytics = FakeAnalyticsTracker()
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
        every { monetizationConfig.getOverviewBannerAdUnitId() } returns "test_ad_unit_id"
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
        every { observeActiveNotificationsUseCase(any()) } returns flowOf(
            es.joshluq.kmsafe.domain.usecase.ObserveActiveNotificationsUseCase.Output.Success(emptyList())
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
            observeActiveNotificationsUseCase = observeActiveNotificationsUseCase,
            markNotificationAsReadUseCase = markNotificationAsReadUseCase,
            publishNotificationIfUnreadUseCase = publishNotificationIfUnreadUseCase,
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
    fun `given switch vehicle clicked then immediately closes switcher and invokes selectContractUseCase`() = runTest(testDispatcher) {
        every { selectContractUseCase(any()) } returns flowOf(
            SelectContractUseCase.Output.Success
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnToggleVehicleSwitcher)
        assertTrue(viewModel.state.value.showVehicleSwitcher)

        viewModel.sendEvent(Event.OnSwitchVehicleClicked("c2"))

        // Switcher must close immediately upon click without waiting for coroutine completion
        assertFalse(viewModel.state.value.showVehicleSwitcher)

        advanceUntilIdle()
        assertFalse(viewModel.state.value.showVehicleSwitcher)
        assertFalse(viewModel.state.value.isLoading)
        coVerify(exactly = 1) { selectContractUseCase(match { it.id == "c2" }) }
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
    fun `given cancel tracked trip clicked then stops service and clears tracking data`() = runTest(testDispatcher) {
        val effects = mutableListOf<Effect>()
        val viewModel = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnCancelTrackedTripClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { stopTripTrackingUseCase(StopTripTrackingUseCase.Input) }
        coVerify(exactly = 1) { clearTrackingUseCase(ClearTrackingUseCase.Input) }
        assertTrue(effects.contains(Effect.DismissTrackingNotifications))
    }

    @Test
    fun `given confirm tracked trip clicked then stops service, prepares bottom sheet and stops tracking`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.sendEvent(Event.OnConfirmTrackedTripClicked)
        advanceUntilIdle()

        coVerify(exactly = 1) { stopTripTrackingUseCase(StopTripTrackingUseCase.Input) }
        coVerify(exactly = 1) { stopTrackingUseCase(StopTrackingUseCase.Input) }
        assertTrue(viewModel.state.value.showBottomSheet)
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

    @Test
    fun `given OnSwitchVehicleClicked when contract exists then triggers switch with overlay state and completes`() = runTest(testDispatcher) {
        val targetVehicle = sampleContract.copy(id = "contract-2", vehicleName = "Peugeot 3008")
        every { getAllContractsUseCase(any()) } returns flowOf(
            GetAllContractsUseCase.Output.Success(listOf(sampleContract, targetVehicle))
        )
        every { selectContractUseCase(any()) } returns flowOf(
            SelectContractUseCase.Output.Progress,
            SelectContractUseCase.Output.Success
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.sendEvent(Event.OnSwitchVehicleClicked("contract-2"))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSwitchingVehicle)
        assertEquals(null, viewModel.state.value.switchingVehicleName)
        coVerify(exactly = 1) { selectContractUseCase(SelectContractUseCase.Input(id = "contract-2", targetVehicleName = "Peugeot 3008")) }
    }

    @Test
    fun `given tracking update when bottom sheet is open then suppresses isTracking in state`() = runTest(testDispatcher) {
        val trackingFlow = MutableSharedFlow<ObserveTrackingStateUseCase.Output>()
        every { observeTrackingStateUseCase(any()) } returns trackingFlow

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Open bottom sheet
        viewModel.sendEvent(Event.OnUpdateOdometerClicked)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.showBottomSheet)

        // Emit active tracking from background
        trackingFlow.emit(
            ObserveTrackingStateUseCase.Output.Success(
                isTracking = true,
                trackedDistance = 100.0,
                startTime = 1000L,
                encodedPolyline = null,
                pointCount = 1
            )
        )
        advanceUntilIdle()

        // Verify isTracking remains false in UI state because bottom sheet is open
        assertFalse(viewModel.state.value.isTracking)
    }

    @Test
    fun `given projection notification when pill clicked then marks read and emits NavigateToProjection effect`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val sampleNotif = es.joshluq.kmsafe.domain.model.Notification(
            id = "proj-pill-1",
            topic = es.joshluq.kmsafe.domain.model.NotificationTopic.PROJECTION,
            title = "Alerta de proyección",
            body = "Revisar cálculo",
            priority = es.joshluq.kmsafe.domain.model.NotificationPriority.CRITICAL,
            status = es.joshluq.kmsafe.domain.model.NotificationStatus.UNREAD,
            deepLinkUri = "kmsafe://feature/projection"
        )

        var emittedEffect: Effect? = null
        val job = launch {
            viewModel.effects.collect { emittedEffect = it }
        }

        viewModel.sendEvent(Event.OnNotificationPillClicked(sampleNotif))
        advanceUntilIdle()

        coVerify(exactly = 1) {
            markNotificationAsReadUseCase(es.joshluq.kmsafe.domain.usecase.MarkNotificationAsReadUseCase.Input("proj-pill-1"))
        }
        assertTrue(emittedEffect is Effect.NavigateToProjection)
        assertEquals(null, viewModel.state.value.activeNotification)
        job.cancel()
    }

    @Test
    fun `given system bluetooth notification when pill clicked then marks read and emits NavigateToOnboarding effect`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val sampleNotif = es.joshluq.kmsafe.domain.model.Notification(
            id = "bt-pill-1",
            topic = es.joshluq.kmsafe.domain.model.NotificationTopic.SYSTEM,
            title = "Dispositivo Bluetooth no configurado",
            body = "Configura el Bluetooth",
            priority = es.joshluq.kmsafe.domain.model.NotificationPriority.WARNING,
            status = es.joshluq.kmsafe.domain.model.NotificationStatus.UNREAD,
            data = mapOf("contract_id" to "contract-1")
        )

        var emittedEffect: Effect? = null
        val job = launch {
            viewModel.effects.collect { emittedEffect = it }
        }

        viewModel.sendEvent(Event.OnNotificationPillClicked(sampleNotif))
        advanceUntilIdle()

        coVerify(exactly = 1) {
            markNotificationAsReadUseCase(es.joshluq.kmsafe.domain.usecase.MarkNotificationAsReadUseCase.Input("bt-pill-1"))
        }
        assertTrue(emittedEffect is Effect.NavigateToOnboarding)
        assertEquals("contract-1", (emittedEffect as Effect.NavigateToOnboarding).vehicleId)
        assertTrue((emittedEffect as Effect.NavigateToOnboarding).isEdit)
        assertEquals(null, viewModel.state.value.activeNotification)
        job.cancel()
    }

    @Test
    fun `given notifications list containing only read notifications then activeNotification is null`() = runTest(testDispatcher) {
        val readNotif = es.joshluq.kmsafe.domain.model.Notification(
            id = "read-1",
            topic = es.joshluq.kmsafe.domain.model.NotificationTopic.PROJECTION,
            title = "Alerta ya leida",
            body = "Detalle",
            priority = es.joshluq.kmsafe.domain.model.NotificationPriority.INFO,
            status = es.joshluq.kmsafe.domain.model.NotificationStatus.READ,
            isRead = true
        )
        every { observeActiveNotificationsUseCase(any()) } returns flowOf(
            es.joshluq.kmsafe.domain.usecase.ObserveActiveNotificationsUseCase.Output.Success(listOf(readNotif))
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(null, viewModel.state.value.activeNotification)
    }

    @Test
    fun `when view all notifications clicked then emits NavigateToNotificationsList effect`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        var emittedEffect: Effect? = null
        val job = launch {
            viewModel.effects.collect { emittedEffect = it }
        }

        viewModel.sendEvent(Event.OnViewAllNotificationsClicked)
        advanceUntilIdle()

        assertTrue(emittedEffect is Effect.NavigateToNotificationsList)
        job.cancel()
    }

    @Test
    fun `given projection transitions from safe to overlimit then publishes critical notification`() = runTest(testDispatcher) {
        val projectionFlow = MutableSharedFlow<GetTripProjectionUseCase.Output>()
        every { getTripProjectionUseCase(any()) } returns projectionFlow
        every { getPreferencesUseCase(any()) } returns flowOf(
            GetPreferencesUseCase.Output.Success(UserPreferences(lastKnownOverLimit = false))
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val overlimitProj = TripProjection(
            contractId = sampleContract.id,
            projectedTotalKms = 25500.0,
            expectedFinalBalance = -500.0,
            isOverLimit = true,
            dailyAverage = 50.0,
            hasEnoughData = true
        )
        projectionFlow.emit(GetTripProjectionUseCase.Output.Success(overlimitProj))
        advanceUntilIdle()

        coVerify(atLeast = 1) {
            publishNotificationIfUnreadUseCase(match { input ->
                input.notification.topic == NotificationTopic.PROJECTION &&
                input.notification.priority == NotificationPriority.CRITICAL &&
                input.notification.title == "Alerta de exceso proyectado"
            })
        }
    }

    @Test
    fun `given projection transitions from overlimit to safe then publishes info notification`() = runTest(testDispatcher) {
        val projectionFlow = MutableSharedFlow<GetTripProjectionUseCase.Output>()
        every { getTripProjectionUseCase(any()) } returns projectionFlow
        every { getPreferencesUseCase(any()) } returns flowOf(
            GetPreferencesUseCase.Output.Success(UserPreferences(lastKnownOverLimit = true))
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val safeProj = TripProjection(
            contractId = sampleContract.id,
            projectedTotalKms = 24800.0,
            expectedFinalBalance = 200.0,
            isOverLimit = false,
            dailyAverage = 40.0,
            hasEnoughData = true
        )
        projectionFlow.emit(GetTripProjectionUseCase.Output.Success(safeProj))
        advanceUntilIdle()

        coVerify(atLeast = 1) {
            publishNotificationIfUnreadUseCase(match { input ->
                input.notification.topic == NotificationTopic.PROJECTION &&
                input.notification.priority == NotificationPriority.INFO &&
                input.notification.title == "Ritmo de kilometraje recuperado"
            })
        }
    }

    @Test
    fun `given projection remains in same state then does not publish duplicate notification`() = runTest(testDispatcher) {
        val projectionFlow = MutableSharedFlow<GetTripProjectionUseCase.Output>()
        every { getTripProjectionUseCase(any()) } returns projectionFlow
        every { getPreferencesUseCase(any()) } returns flowOf(
            GetPreferencesUseCase.Output.Success(UserPreferences(lastKnownOverLimit = true))
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val overlimitProj = TripProjection(
            contractId = sampleContract.id,
            projectedTotalKms = 25500.0,
            expectedFinalBalance = -500.0,
            isOverLimit = true,
            dailyAverage = 50.0,
            hasEnoughData = true
        )
        projectionFlow.emit(GetTripProjectionUseCase.Output.Success(overlimitProj))
        advanceUntilIdle()

        coVerify(exactly = 0) {
            publishNotificationIfUnreadUseCase(any())
        }
    }

    @Test
    fun `given premium user with autotracking and vehicle without bluetooth then publishes warning notification`() = runTest(testDispatcher) {
        val contractNoBt = sampleContract.copy(bluetoothDeviceAddress = null)
        val overviewFlow = flowOf(
            GetOverviewDataUseCase.Output.Success(
                contract = contractNoBt,
                metrics = sampleMetrics.copy(contract = contractNoBt),
                actualKmsDrivenSinceStart = 2500.0
            )
        )
        every { getOverviewDataUseCase(any()) } returns overviewFlow
        every { getPreferencesUseCase(any()) } returns flowOf(
            GetPreferencesUseCase.Output.Success(UserPreferences(autoTrackingEnabled = true))
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        coVerify(atLeast = 1) {
            publishNotificationIfUnreadUseCase(match { input ->
                input.notification.topic == NotificationTopic.SYSTEM &&
                input.notification.priority == NotificationPriority.WARNING &&
                input.notification.title == "Dispositivo Bluetooth no configurado"
            })
        }
    }

    @Test
    fun `given vehicle without bluetooth subsequently gets bluetooth configured then resolves notification`() = runTest(testDispatcher) {
        val contractNoBt = sampleContract.copy(bluetoothDeviceAddress = null)
        val overviewFlow = MutableSharedFlow<GetOverviewDataUseCase.Output>()
        every { getOverviewDataUseCase(any()) } returns overviewFlow
        every { getPreferencesUseCase(any()) } returns flowOf(
            GetPreferencesUseCase.Output.Success(UserPreferences(autoTrackingEnabled = true))
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        // 1. Emit contract without bluetooth -> publishes warning
        overviewFlow.emit(
            GetOverviewDataUseCase.Output.Success(
                contract = contractNoBt,
                metrics = sampleMetrics.copy(contract = contractNoBt),
                actualKmsDrivenSinceStart = 2500.0
            )
        )
        advanceUntilIdle()

        coVerify(atLeast = 1) {
            publishNotificationIfUnreadUseCase(any())
        }

        // 2. Emit contract with bluetooth -> resolves notification
        val contractWithBt = sampleContract.copy(bluetoothDeviceAddress = "AA:BB:CC:DD:EE:FF")
        overviewFlow.emit(
            GetOverviewDataUseCase.Output.Success(
                contract = contractWithBt,
                metrics = sampleMetrics.copy(contract = contractWithBt),
                actualKmsDrivenSinceStart = 2500.0
            )
        )
        advanceUntilIdle()

        coVerify(atLeast = 1) {
            markNotificationAsReadUseCase(match { it.notificationId == "bt_missing_${sampleContract.id}" })
        }
    }
}
