package es.joshluq.kmsafe.feature.auth.login

import es.joshluq.kmsafe.core.analytics.fake.FakeAnalyticsTracker
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.model.UserPreferences
import es.joshluq.kmsafe.domain.service.FingerprintProvider
import es.joshluq.kmsafe.domain.service.SocialAuthService
import es.joshluq.kmsafe.domain.usecase.ClearLocalDataUseCase
import es.joshluq.kmsafe.domain.usecase.EvaluateIdentityConflictUseCase
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.GetPreferencesUseCase
import es.joshluq.kmsafe.domain.usecase.SignInUseCase
import es.joshluq.kmsafe.domain.usecase.SignInWithGoogleUseCase
import es.joshluq.kmsafe.domain.usecase.SyncContractsUseCase
import es.joshluq.kmsafe.domain.usecase.UpdatePreferencesUseCase
import es.joshluq.kmsafe.domain.usecase.ValidateCredentialsUseCase
import es.joshluq.kmsafe.feature.auth.domain.AuthConfig
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val validateCredentialsUseCase: ValidateCredentialsUseCase = mockk()
    private val signInUseCase: SignInUseCase = mockk()
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase = mockk()
    private val syncContractsUseCase: SyncContractsUseCase = mockk()
    private val clearLocalDataUseCase: ClearLocalDataUseCase = mockk()
    private val evaluateIdentityConflictUseCase: EvaluateIdentityConflictUseCase = mockk()
    private val getPreferencesUseCase: GetPreferencesUseCase = mockk()
    private val updatePreferencesUseCase: UpdatePreferencesUseCase = mockk()
    private val getEntitlementsUseCase: GetEntitlementsUseCase = mockk()
    private val fingerprintProvider: FingerprintProvider = mockk()
    private val socialAuthService: SocialAuthService = mockk()
    private val authConfig: AuthConfig = mockk()
    private val analytics = FakeAnalyticsTracker()
    private val logger: LoggerKit = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { authConfig.getTermsUrl() } returns "https://example.com/terms"
        every { authConfig.getPrivacyUrl() } returns "https://example.com/privacy"
        coEvery { fingerprintProvider.getFingerprint() } returns "test_fp"
        coEvery { validateCredentialsUseCase(any()) } returns Result.success(
            ValidateCredentialsUseCase.Output(isEmailValid = true, isPasswordValid = true, canLogin = true)
        )
        every { getPreferencesUseCase(GetPreferencesUseCase.Input) } returns flowOf(
            GetPreferencesUseCase.Output.Success(UserPreferences())
        )
        every { updatePreferencesUseCase(any()) } returns flowOf(
            UpdatePreferencesUseCase.Output.Success
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun createViewModel(): LoginViewModel {
        return LoginViewModel(
            validateCredentialsUseCase = validateCredentialsUseCase,
            signInUseCase = signInUseCase,
            signInWithGoogleUseCase = signInWithGoogleUseCase,
            syncContractsUseCase = syncContractsUseCase,
            clearLocalDataUseCase = clearLocalDataUseCase,
            evaluateIdentityConflictUseCase = evaluateIdentityConflictUseCase,
            getPreferencesUseCase = getPreferencesUseCase,
            updatePreferencesUseCase = updatePreferencesUseCase,
            getEntitlementsUseCase = getEntitlementsUseCase,
            fingerprintProvider = fingerprintProvider,
            socialAuthService = socialAuthService,
            authConfig = authConfig,
            analytics = analytics,
            logger = logger
        )
    }

    @Test
    fun `given email and password input when valid then enables login button`() = runTest(testDispatcher) {
        coEvery { validateCredentialsUseCase(any()) } returns Result.success(
            ValidateCredentialsUseCase.Output(isEmailValid = true, isPasswordValid = true, canLogin = true)
        )

        val viewModel = createViewModel()
        viewModel.sendEvent(Event.OnEmailChanged("test@example.com"))
        viewModel.sendEvent(Event.OnPasswordChanged("Password123!"))
        advanceUntilIdle()

        assertEquals("test@example.com", viewModel.state.value.email)
        assertEquals("Password123!", viewModel.state.value.password)
        assertTrue(viewModel.state.value.isLoginEnabled)
    }

    @Test
    fun `given password visibility toggle when triggered then flips isPasswordVisible`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        assertFalse(viewModel.state.value.isPasswordVisible)

        viewModel.sendEvent(Event.OnTogglePasswordVisibility)
        assertTrue(viewModel.state.value.isPasswordVisible)

        viewModel.sendEvent(Event.OnTogglePasswordVisibility)
        assertFalse(viewModel.state.value.isPasswordVisible)
    }

    @Test
    fun `given successful login for premium user when login clicked then navigates to dashboard`() = runTest(testDispatcher) {
        coEvery { validateCredentialsUseCase(any()) } returns Result.success(
            ValidateCredentialsUseCase.Output(isEmailValid = true, isPasswordValid = true, canLogin = true)
        )
        every { evaluateIdentityConflictUseCase(any()) } returns flowOf(
            EvaluateIdentityConflictUseCase.Output.NoConflict
        )
        val testUser = User(id = "u1", email = "test@example.com", name = "Test User")
        every { signInUseCase(any()) } returns flowOf(
            SignInUseCase.Output.Success(testUser)
        )
        every { getEntitlementsUseCase(any()) } returns flowOf(
            GetEntitlementsUseCase.Output.Success(Entitlements.Default.copy(subscriptionLevel = SubscriptionLevel.PREMIUM))
        )
        every { syncContractsUseCase(any()) } returns flowOf(
            SyncContractsUseCase.Output.Success
        )

        val effects = mutableListOf<Effect>()
        val viewModel = createViewModel()
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnEmailChanged("test@example.com"))
        viewModel.sendEvent(Event.OnPasswordChanged("pass"))
        viewModel.sendEvent(Event.OnLoginClicked)
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(Effect.NavigateToDashboard, effects.first())
    }

    @Test
    fun `given successful login for free user when login clicked then navigates to paywall`() = runTest(testDispatcher) {
        coEvery { validateCredentialsUseCase(any()) } returns Result.success(
            ValidateCredentialsUseCase.Output(isEmailValid = true, isPasswordValid = true, canLogin = true)
        )
        every { evaluateIdentityConflictUseCase(any()) } returns flowOf(
            EvaluateIdentityConflictUseCase.Output.NoConflict
        )
        val testUser = User(id = "u1", email = "free@example.com", name = "Free User")
        every { signInUseCase(any()) } returns flowOf(
            SignInUseCase.Output.Success(testUser)
        )
        every { getEntitlementsUseCase(any()) } returns flowOf(
            GetEntitlementsUseCase.Output.Success(Entitlements.Default.copy(subscriptionLevel = SubscriptionLevel.FREE))
        )
        every { syncContractsUseCase(any()) } returns flowOf(
            SyncContractsUseCase.Output.Success
        )

        val effects = mutableListOf<Effect>()
        val viewModel = createViewModel()
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnEmailChanged("free@example.com"))
        viewModel.sendEvent(Event.OnPasswordChanged("pass"))
        viewModel.sendEvent(Event.OnLoginClicked)
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(Effect.NavigateToPremiumPaywall, effects.first())
    }

    @Test
    fun `given invalid credentials when login clicked then shows error in state`() = runTest(testDispatcher) {
        every { evaluateIdentityConflictUseCase(any()) } returns flowOf(
            EvaluateIdentityConflictUseCase.Output.NoConflict
        )
        every { signInUseCase(any()) } returns flowOf(
            SignInUseCase.Output.Failure(KmError.InvalidCredentials)
        )

        val viewModel = createViewModel()
        viewModel.sendEvent(Event.OnEmailChanged("wrong@example.com"))
        viewModel.sendEvent(Event.OnPasswordChanged("wrong"))
        viewModel.sendEvent(Event.OnLoginClicked)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `given identity conflict when login clicked then displays warning dialog`() = runTest(testDispatcher) {
        every { evaluateIdentityConflictUseCase(any()) } returns flowOf(
            EvaluateIdentityConflictUseCase.Output.ShowWarning
        )

        val viewModel = createViewModel()
        viewModel.sendEvent(Event.OnEmailChanged("different@example.com"))
        viewModel.sendEvent(Event.OnPasswordChanged("pass"))
        viewModel.sendEvent(Event.OnLoginClicked)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.showUserConflictWarning)
        assertFalse(viewModel.state.value.isLoading)
    }
}
