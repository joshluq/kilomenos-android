package es.joshluq.kmsafe.feature.profile.preferences

import es.joshluq.kmsafe.core.analytics.fake.FakeAnalyticsTracker
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.core.monetization.util.ConsentManager
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.UserPreferences
import es.joshluq.kmsafe.domain.service.FingerprintProvider
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.GetPreferencesUseCase
import es.joshluq.kmsafe.domain.usecase.StartAutoTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StartTrialUseCase
import es.joshluq.kmsafe.domain.usecase.StopAutoTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.UpdatePreferencesUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val getPreferencesUseCase: GetPreferencesUseCase = mockk()
    private val updatePreferencesUseCase: UpdatePreferencesUseCase = mockk()
    private val getEntitlementsUseCase: GetEntitlementsUseCase = mockk()
    private val startTrialUseCase: StartTrialUseCase = mockk()
    private val fingerprintProvider: FingerprintProvider = mockk()
    private val startAutoTrackingUseCase: StartAutoTrackingUseCase = mockk()
    private val stopAutoTrackingUseCase: StopAutoTrackingUseCase = mockk()
    private val consentManager: ConsentManager = mockk()
    private val analytics = FakeAnalyticsTracker()
    private val logger: LoggerKit = mockk(relaxed = true)

    private val initialPreferences = UserPreferences(
        rememberEmail = true,
        showProjectionBanner = true,
        autoTrackingEnabled = false
    )

    private val premiumEntitlements = Entitlements(
        subscriptionLevel = SubscriptionLevel.PREMIUM,
        isTrialActive = false,
        trialExpiresAt = null,
        enabledFeatures = setOf(Feature.AUTO_TRACKING),
        canStartTrial = false,
        trialFeatures = emptySet()
    )

    private val freeTrialableEntitlements = Entitlements(
        subscriptionLevel = SubscriptionLevel.FREE,
        isTrialActive = false,
        trialExpiresAt = null,
        enabledFeatures = emptySet(),
        canStartTrial = true,
        trialFeatures = setOf(Feature.AUTO_TRACKING)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { consentManager.isPrivacyOptionsRequired() } returns true
        every { fingerprintProvider.getFingerprint() } returns "test-fingerprint"
        every { getPreferencesUseCase(any()) } returns flowOf(GetPreferencesUseCase.Output.Success(initialPreferences))
        every { getEntitlementsUseCase(any()) } returns flowOf(GetEntitlementsUseCase.Output.Success(premiumEntitlements))
        every { updatePreferencesUseCase(any()) } returns flowOf(UpdatePreferencesUseCase.Output.Success)
        every { startAutoTrackingUseCase(any()) } returns flowOf(StartAutoTrackingUseCase.Output.Success)
        every { stopAutoTrackingUseCase(any()) } returns flowOf(StopAutoTrackingUseCase.Output.Success)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PreferencesViewModel {
        return PreferencesViewModel(
            getPreferencesUseCase = getPreferencesUseCase,
            updatePreferencesUseCase = updatePreferencesUseCase,
            getEntitlementsUseCase = getEntitlementsUseCase,
            startTrialUseCase = startTrialUseCase,
            fingerprintProvider = fingerprintProvider,
            startAutoTrackingUseCase = startAutoTrackingUseCase,
            stopAutoTrackingUseCase = stopAutoTrackingUseCase,
            consentManager = consentManager,
            analytics = analytics,
            logger = logger
        )
    }

    @Test
    fun `init loads preferences and entitlements correctly`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.rememberEmail)
        assertTrue(state.showProjectionBanner)
        assertFalse(state.autoTrackingEnabled)
        assertTrue(state.isUserPremium)
        assertFalse(state.canStartTrial)
        assertTrue(state.isPrivacyOptionsRequired)
        assertTrue(state.isEntitlementsLoaded)
    }

    @Test
    fun `self-healing disables auto-tracking if user is free and autoTrackingEnabled is true`() = runTest(testDispatcher) {
        val freePreferences = initialPreferences.copy(autoTrackingEnabled = true)
        val freeEntitlements = Entitlements(
            subscriptionLevel = SubscriptionLevel.FREE,
            isTrialActive = false,
            trialExpiresAt = null,
            enabledFeatures = emptySet(),
            canStartTrial = false,
            trialFeatures = emptySet()
        )

        every { getPreferencesUseCase(any()) } returns flowOf(GetPreferencesUseCase.Output.Success(freePreferences))
        every { getEntitlementsUseCase(any()) } returns flowOf(GetEntitlementsUseCase.Output.Success(freeEntitlements))

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.autoTrackingEnabled)
        verify { updatePreferencesUseCase(UpdatePreferencesUseCase.Input(autoTrackingEnabled = false)) }
        verify { stopAutoTrackingUseCase(StopAutoTrackingUseCase.Input) }
    }

    @Test
    fun `remember email toggle updates preferences`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.sendEvent(Event.OnRememberEmailToggled(false))
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.rememberEmail)
        verify { updatePreferencesUseCase(UpdatePreferencesUseCase.Input(rememberEmail = false)) }
    }

    @Test
    fun `projection banner toggle updates preferences`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.sendEvent(Event.OnProjectionBannerToggled(false))
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.showProjectionBanner)
        verify { updatePreferencesUseCase(UpdatePreferencesUseCase.Input(showProjectionBanner = false)) }
    }

    @Test
    fun `auto-tracking toggle shows trial offer when user is free and trial is available`() = runTest(testDispatcher) {
        every { getEntitlementsUseCase(any()) } returns flowOf(GetEntitlementsUseCase.Output.Success(freeTrialableEntitlements))

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.sendEvent(Event.OnAutoTrackingToggled(true))
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.showTrialOffer)

        viewModel.sendEvent(Event.OnDismissTrialOffer)
        assertFalse(viewModel.state.value.showTrialOffer)
    }

    @Test
    fun `auto-tracking toggle navigates to permissions when user is premium`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val effects = mutableListOf<Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnAutoTrackingToggled(true))
        testScheduler.advanceUntilIdle()

        assertEquals(listOf(Effect.NavigateToPermissions), effects)
        assertTrue(viewModel.state.value.autoTrackingEnabled)
    }

    @Test
    fun `permissions result true executes auto-tracking enabled and starts service`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.sendEvent(Event.OnPermissionsResult(true))
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.autoTrackingEnabled)
        verify { updatePreferencesUseCase(UpdatePreferencesUseCase.Input(autoTrackingEnabled = true)) }
        verify { startAutoTrackingUseCase(StartAutoTrackingUseCase.Input) }
    }

    @Test
    fun `permissions result false ensures auto-tracking is disabled`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.sendEvent(Event.OnPermissionsResult(false))
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.autoTrackingEnabled)
    }

    @Test
    fun `start trial success emits navigate to permissions`() = runTest(testDispatcher) {
        every { startTrialUseCase(StartTrialUseCase.Input("test-fingerprint")) } returns flowOf(
            StartTrialUseCase.Output.Success(premiumEntitlements)
        )

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val effects = mutableListOf<Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnStartTrialClicked)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(listOf(Effect.NavigateToPermissions), effects)
    }

    @Test
    fun `start trial failure sets error and dismiss error clears it`() = runTest(testDispatcher) {
        every { startTrialUseCase(StartTrialUseCase.Input("test-fingerprint")) } returns flowOf(
            StartTrialUseCase.Output.Failure(KmError.UnknownError)
        )

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.sendEvent(Event.OnStartTrialClicked)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.error != null)

        viewModel.sendEvent(Event.OnDismissError)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `simple navigation events emit expected effects`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val effects = mutableListOf<Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnManagePrivacyClicked)
        viewModel.sendEvent(Event.OnBackClicked)

        testScheduler.advanceUntilIdle()

        assertEquals(listOf(Effect.ShowPrivacyOptions, Effect.NavigateBack), effects)
    }
}
