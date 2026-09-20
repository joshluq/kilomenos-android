package es.joshluq.kmsafe.feature.premium.paywall

import es.joshluq.kmsafe.core.analytics.fake.FakeAnalyticsTracker
import es.joshluq.kmsafe.core.analytics.model.KmAnalyticsEvent
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.service.BillingService
import es.joshluq.kmsafe.domain.usecase.MigrateLocalDataToRemoteUseCase
import es.joshluq.kmsafe.domain.usecase.RestorePurchasesUseCase
import es.joshluq.kmsafe.domain.usecase.SyncContractsUseCase
import es.joshluq.kmsafe.domain.usecase.UpdateSubscriptionUseCase
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.kmsafe.feature.premium.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PremiumPaywallViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val billingService: BillingService = mockk()
    private val updateSubscriptionUseCase: UpdateSubscriptionUseCase = mockk()
    private val restorePurchasesUseCase: RestorePurchasesUseCase = mockk()
    private val migrateLocalDataUseCase: MigrateLocalDataToRemoteUseCase = mockk()
    private val syncContractsUseCase: SyncContractsUseCase = mockk()
    private val analytics = FakeAnalyticsTracker()
    private val logger: LoggerKit = mockk(relaxed = true)

    private val purchaseProcessingFlow = MutableSharedFlow<Boolean>()
    private val purchaseSuccessFlow = MutableSharedFlow<String>()
    private val billingErrorFlow = MutableSharedFlow<String>()

    private val sampleUser = User(
        id = "user-1",
        email = "test@example.com",
        name = "Test User"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { billingService.purchaseProcessingFlow } returns purchaseProcessingFlow
        every { billingService.purchaseSuccessFlow } returns purchaseSuccessFlow
        every { billingService.errorFlow } returns billingErrorFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PremiumPaywallViewModel {
        return PremiumPaywallViewModel(
            billingService = billingService,
            updateSubscriptionUseCase = updateSubscriptionUseCase,
            restorePurchasesUseCase = restorePurchasesUseCase,
            migrateLocalDataUseCase = migrateLocalDataUseCase,
            syncContractsUseCase = syncContractsUseCase,
            analytics = analytics,
            logger = logger
        )
    }

    @Test
    fun `initial state is empty and tracks screen view`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isMigrating)
        assertFalse(state.isRestoring)
        assertEquals(PremiumBillingPlan.MONTHLY, state.selectedPlan)
        assertNull(state.error)
        assertNull(state.message)
    }

    @Test
    fun `on initialize with source updates state and tracks paywall viewed with source`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.sendEvent(Event.OnInitialize("projection_risk_sentinel"))
        testScheduler.advanceUntilIdle()

        assertEquals("projection_risk_sentinel", viewModel.state.value.source)
        val viewedEvent = analytics.trackedEvents.filterIsInstance<KmAnalyticsEvent.Monetization.PaywallViewed>().firstOrNull()
        assertTrue(viewedEvent != null)
        assertEquals("projection_risk_sentinel", viewedEvent?.source)
    }

    @Test
    fun `on plan selected updates state and tracks analytics`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.sendEvent(Event.OnPlanSelected(PremiumBillingPlan.ANNUAL))
        testScheduler.advanceUntilIdle()

        assertEquals(PremiumBillingPlan.ANNUAL, viewModel.state.value.selectedPlan)
        assertTrue(analytics.trackedEvents.any { it is KmAnalyticsEvent.Monetization.PlanSelected && it.plan == "ANNUAL" })
    }

    @Test
    fun `on upgrade clicked emits launch billing flow effect and tracks plan`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val effects = mutableListOf<Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnUpgradeClicked)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(Effect.LaunchBillingFlow), effects)
        assertTrue(analytics.trackedEvents.any { it is KmAnalyticsEvent.Monetization.UpgradeClicked && it.selectedPlan == "MONTHLY" })
    }

    @Test
    fun `on dismiss clicked emits navigate back effect`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val effects = mutableListOf<Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnDismissClicked)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(Effect.NavigateBack), effects)
    }

    @Test
    fun `purchase success updates subscription, migrates data, syncs, and navigates to dashboard`() = runTest(testDispatcher) {
        every { updateSubscriptionUseCase(UpdateSubscriptionUseCase.Input(SubscriptionLevel.PREMIUM)) } returns flowOf(
            UpdateSubscriptionUseCase.Output.Progress,
            UpdateSubscriptionUseCase.Output.Success(sampleUser)
        )
        every { migrateLocalDataUseCase(MigrateLocalDataToRemoteUseCase.Input) } returns flowOf(
            MigrateLocalDataToRemoteUseCase.Output.Progress,
            MigrateLocalDataToRemoteUseCase.Output.Success
        )
        every { syncContractsUseCase(SyncContractsUseCase.Input) } returns flowOf(
            SyncContractsUseCase.Output.Progress,
            SyncContractsUseCase.Output.Success
        )

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val effects = mutableListOf<Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        purchaseSuccessFlow.emit("order-12345")
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isMigrating)
        assertEquals(listOf(Effect.NavigateToDashboard), effects)
        verify { updateSubscriptionUseCase(UpdateSubscriptionUseCase.Input(SubscriptionLevel.PREMIUM)) }
        verify { migrateLocalDataUseCase(MigrateLocalDataToRemoteUseCase.Input) }
        verify { syncContractsUseCase(SyncContractsUseCase.Input) }
    }

    @Test
    fun `purchase success with update subscription failure shows error`() = runTest(testDispatcher) {
        every { updateSubscriptionUseCase(UpdateSubscriptionUseCase.Input(SubscriptionLevel.PREMIUM)) } returns flowOf(
            UpdateSubscriptionUseCase.Output.Progress,
            UpdateSubscriptionUseCase.Output.Failure
        )

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        purchaseSuccessFlow.emit("order-12345")
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.error != null)

        viewModel.sendEvent(Event.OnDismissError)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `billing error flow updates state with error`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        billingErrorFlow.emit("Billing service unavailable")
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.error != null)
    }

    @Test
    fun `restore purchases success migrates data, syncs, sets message and navigates`() = runTest(testDispatcher) {
        every { restorePurchasesUseCase(RestorePurchasesUseCase.Input) } returns flowOf(
            RestorePurchasesUseCase.Output.Progress,
            RestorePurchasesUseCase.Output.Success(restoredCount = 1)
        )
        every { migrateLocalDataUseCase(MigrateLocalDataToRemoteUseCase.Input) } returns flowOf(
            MigrateLocalDataToRemoteUseCase.Output.Progress,
            MigrateLocalDataToRemoteUseCase.Output.Success
        )
        every { syncContractsUseCase(SyncContractsUseCase.Input) } returns flowOf(
            SyncContractsUseCase.Output.Progress,
            SyncContractsUseCase.Output.Success
        )

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val effects = mutableListOf<Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnRestorePurchasesClicked)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isRestoring)
        assertTrue(viewModel.state.value.message != null)
        assertEquals(listOf(Effect.NavigateToDashboard), effects)
        assertTrue(analytics.trackedEvents.any { it is KmAnalyticsEvent.Monetization.UpgradeSuccess })
    }

    @Test
    fun `restore purchases with no active subscriptions shows empty message`() = runTest(testDispatcher) {
        every { restorePurchasesUseCase(RestorePurchasesUseCase.Input) } returns flowOf(
            RestorePurchasesUseCase.Output.Progress,
            RestorePurchasesUseCase.Output.NoPurchasesFound
        )

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.sendEvent(Event.OnRestorePurchasesClicked)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isRestoring)
        assertTrue(viewModel.state.value.message != null)

        viewModel.sendEvent(Event.OnDismissMessage)
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `restore purchases failure shows error`() = runTest(testDispatcher) {
        every { restorePurchasesUseCase(RestorePurchasesUseCase.Input) } returns flowOf(
            RestorePurchasesUseCase.Output.Progress,
            RestorePurchasesUseCase.Output.Failure("Network error")
        )

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.sendEvent(Event.OnRestorePurchasesClicked)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isRestoring)
        assertTrue(viewModel.state.value.error != null)
    }

    @Test
    fun `purchaseProcessingFlow emitting true immediately sets isLoading to true`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        purchaseProcessingFlow.emit(true)
        testScheduler.runCurrent()

        assertTrue(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `purchaseProcessingFlow emitting false sets isLoading to false`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        purchaseProcessingFlow.emit(true)
        testScheduler.runCurrent()
        assertTrue(viewModel.state.value.isLoading)

        purchaseProcessingFlow.emit(false)
        testScheduler.runCurrent()
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `events are ignored when isLoading is true`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        purchaseProcessingFlow.emit(true)
        testScheduler.runCurrent()
        assertTrue(viewModel.state.value.isLoading)

        // Try selecting plan
        viewModel.sendEvent(Event.OnPlanSelected(PremiumBillingPlan.ANNUAL))
        testScheduler.runCurrent()
        assertEquals(PremiumBillingPlan.MONTHLY, viewModel.state.value.selectedPlan)

        // Try upgrade
        val effects = mutableListOf<Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }
        viewModel.sendEvent(Event.OnUpgradeClicked)
        testScheduler.runCurrent()
        assertTrue(effects.isEmpty())

        // Try dismiss
        viewModel.sendEvent(Event.OnDismissClicked)
        testScheduler.runCurrent()
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `loading watchdog triggers after 25 seconds and resets isLoading with error`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        purchaseProcessingFlow.emit(true)
        testScheduler.runCurrent()
        assertTrue(viewModel.state.value.isLoading)

        // Advance past watchdog timeout (25_000ms)
        testScheduler.advanceTimeBy(25_001L)
        testScheduler.runCurrent()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(TextProvider.Resource(R.string.premium_operation_timeout), viewModel.state.value.error)
    }
}
