package es.joshluq.kmsafe.feature.profile

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.usecase.DeleteAccountUseCase
import es.joshluq.kmsafe.domain.usecase.GetCurrentUserUseCase
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.SetAppOverlayUseCase
import es.joshluq.kmsafe.domain.usecase.SignOutUseCase
import es.joshluq.kmsafe.feature.profile.domain.ProfileConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val signOutUseCase: SignOutUseCase = mockk()
    private val getCurrentUserUseCase: GetCurrentUserUseCase = mockk()
    private val deleteAccountUseCase: DeleteAccountUseCase = mockk()
    private val getEntitlementsUseCase: GetEntitlementsUseCase = mockk()
    private val setAppOverlayUseCase: SetAppOverlayUseCase = mockk(relaxed = true)
    private val profileConfig: ProfileConfig = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private val sampleUser = User(
        id = "user-1",
        email = "john@example.com",
        name = "John Doe"
    )

    private val sampleEntitlements = Entitlements(
        subscriptionLevel = SubscriptionLevel.PREMIUM,
        isTrialActive = false,
        trialExpiresAt = null,
        enabledFeatures = emptySet(),
        canStartTrial = false,
        trialFeatures = emptySet()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { profileConfig.getTermsUrl() } returns "https://example.com/terms"
        every { profileConfig.getPrivacyUrl() } returns "https://example.com/privacy"
        every { getCurrentUserUseCase(any()) } returns flowOf(GetCurrentUserUseCase.Output.Success(sampleUser))
        every { getEntitlementsUseCase(any()) } returns flowOf(GetEntitlementsUseCase.Output.Success(sampleEntitlements))
        every { setAppOverlayUseCase(any()) } returns flowOf(SetAppOverlayUseCase.Output.Success)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ProfileViewModel {
        return ProfileViewModel(
            signOutUseCase = signOutUseCase,
            getCurrentUserUseCase = getCurrentUserUseCase,
            deleteAccountUseCase = deleteAccountUseCase,
            getEntitlementsUseCase = getEntitlementsUseCase,
            setAppOverlayUseCase = setAppOverlayUseCase,
            profileConfig = profileConfig,
            logger = logger
        )
    }

    @Test
    fun `init loads user, entitlements, and config urls`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(sampleUser, state.user)
        assertEquals(sampleEntitlements, state.entitlements)
        assertEquals("https://example.com/terms", state.termsUrl)
        assertEquals("https://example.com/privacy", state.privacyUrl)
    }

    @Test
    fun `navigation events emit correct effects`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val effects = mutableListOf<Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnVehiclesClicked)
        viewModel.sendEvent(Event.OnDataManagementClicked)
        viewModel.sendEvent(Event.OnPreferencesClicked)
        viewModel.sendEvent(Event.OnUpgradeClicked)
        viewModel.sendEvent(Event.OnWelcomeGuideClicked)

        testScheduler.advanceUntilIdle()

        assertEquals(
            listOf(
                Effect.NavigateToVehicles,
                Effect.NavigateToDataManagement,
                Effect.NavigateToPreferences,
                Effect.NavigateToPremiumPaywall,
                Effect.NavigateToWelcomeDiscovery
            ),
            effects
        )
    }

    @Test
    fun `logout flow toggles confirmation and executes signOut successfully`() = runTest(testDispatcher) {
        every { signOutUseCase(SignOutUseCase.Input(clearLocalData = true)) } returns flowOf(
            SignOutUseCase.Output.Progress,
            SignOutUseCase.Output.Success
        )

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val effects = mutableListOf<Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnLogoutClicked)
        assertTrue(viewModel.state.value.showLogoutConfirmation)

        viewModel.sendEvent(Event.OnLogoutConfirmed)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.showLogoutConfirmation)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.user)
        assertEquals(listOf(Effect.NavigateToLogin), effects)
        verify { signOutUseCase(SignOutUseCase.Input(clearLocalData = true)) }
    }

    @Test
    fun `OnResume re-triggers observation of user and entitlements`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.sendEvent(Event.OnResume)
        testScheduler.advanceUntilIdle()

        verify(atLeast = 2) { getCurrentUserUseCase(any()) }
        verify(atLeast = 2) { getEntitlementsUseCase(any()) }
    }

    @Test
    fun `logout cancel hides confirmation dialog`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.sendEvent(Event.OnLogoutClicked)
        assertTrue(viewModel.state.value.showLogoutConfirmation)

        viewModel.sendEvent(Event.OnLogoutCancelled)
        assertFalse(viewModel.state.value.showLogoutConfirmation)
    }

    @Test
    fun `delete account flow toggles confirmation, advances progress, and navigates to login`() = runTest(testDispatcher) {
        every { deleteAccountUseCase(DeleteAccountUseCase.Input) } returns flowOf(
            DeleteAccountUseCase.Output.Progress,
            DeleteAccountUseCase.Output.Success
        )

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val effects = mutableListOf<Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnDeleteAccountClicked)
        assertTrue(viewModel.state.value.showDeleteConfirmation)

        viewModel.sendEvent(Event.OnDeleteAccountConfirmed)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.showDeleteConfirmation)
        assertEquals(listOf(Effect.NavigateToLogin), effects)
        verify(atLeast = 1) { setAppOverlayUseCase(any()) }
    }

    @Test
    fun `delete account failure updates state with error and dismiss error clears it`() = runTest(testDispatcher) {
        every { deleteAccountUseCase(DeleteAccountUseCase.Input) } returns flowOf(
            DeleteAccountUseCase.Output.Failure
        )

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.sendEvent(Event.OnDeleteAccountConfirmed)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isDeleting)
        assertTrue(viewModel.state.value.error != null)
        verify(atLeast = 1) { setAppOverlayUseCase(any()) }

        viewModel.sendEvent(Event.OnDismissError)
        assertNull(viewModel.state.value.error)
    }
}
