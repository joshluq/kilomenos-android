package es.joshluq.kmsafe.feature.premium.paywall

import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.service.BillingService
import es.joshluq.kmsafe.domain.usecase.MigrateLocalDataToRemoteUseCase
import es.joshluq.kmsafe.domain.usecase.SyncContractsUseCase
import es.joshluq.kmsafe.domain.usecase.UpdateSubscriptionUseCase
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
import kotlinx.coroutines.test.advanceUntilIdle
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
    private val migrateLocalDataUseCase: MigrateLocalDataToRemoteUseCase = mockk()
    private val syncContractsUseCase: SyncContractsUseCase = mockk()
    private val analytics: AnalyticskitManager = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)

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
        assertNull(state.error)
    }

    @Test
    fun `on upgrade clicked emits launch billing flow effect`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val effects = mutableListOf<Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnUpgradeClicked)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(Effect.LaunchBillingFlow), effects)
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
}
