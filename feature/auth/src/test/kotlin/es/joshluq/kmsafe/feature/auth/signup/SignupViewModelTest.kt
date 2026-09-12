package es.joshluq.kmsafe.feature.auth.signup

import es.joshluq.kmsafe.core.analytics.fake.FakeAnalyticsTracker
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.usecase.ClearLocalDataUseCase
import es.joshluq.kmsafe.domain.usecase.EvaluateIdentityConflictUseCase
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.SignUpUseCase
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
class SignupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val validateCredentialsUseCase: ValidateCredentialsUseCase = mockk()
    private val signUpUseCase: SignUpUseCase = mockk()
    private val clearLocalDataUseCase: ClearLocalDataUseCase = mockk()
    private val evaluateIdentityConflictUseCase: EvaluateIdentityConflictUseCase = mockk()
    private val updatePreferencesUseCase: UpdatePreferencesUseCase = mockk()
    private val getEntitlementsUseCase: GetEntitlementsUseCase = mockk()
    private val syncContractsUseCase: SyncContractsUseCase = mockk()
    private val authConfig: AuthConfig = mockk()
    private val analytics = FakeAnalyticsTracker()
    private val logger: LoggerKit = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { authConfig.getTermsUrl() } returns "https://example.com/terms"
        every { authConfig.getPrivacyUrl() } returns "https://example.com/privacy"
        coEvery { validateCredentialsUseCase(any()) } returns Result.success(
            ValidateCredentialsUseCase.Output(isEmailValid = true, isPasswordValid = true, canLogin = true)
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

    private fun createViewModel(): SignupViewModel {
        return SignupViewModel(
            validateCredentialsUseCase = validateCredentialsUseCase,
            signUpUseCase = signUpUseCase,
            clearLocalDataUseCase = clearLocalDataUseCase,
            evaluateIdentityConflictUseCase = evaluateIdentityConflictUseCase,
            updatePreferencesUseCase = updatePreferencesUseCase,
            getEntitlementsUseCase = getEntitlementsUseCase,
            syncContractsUseCase = syncContractsUseCase,
            authConfig = authConfig,
            analytics = analytics,
            logger = logger
        )
    }

    @Test
    fun `given valid name, email and password when entered then enables signup button`() = runTest(testDispatcher) {
        coEvery { validateCredentialsUseCase(any()) } returns Result.success(
            ValidateCredentialsUseCase.Output(isEmailValid = true, isPasswordValid = true, canLogin = true)
        )

        val viewModel = createViewModel()
        viewModel.sendEvent(Event.OnNameChanged("John Doe"))
        viewModel.sendEvent(Event.OnEmailChanged("john@example.com"))
        viewModel.sendEvent(Event.OnPasswordChanged("Password123!"))
        advanceUntilIdle()

        assertEquals("John Doe", viewModel.state.value.name)
        assertEquals("john@example.com", viewModel.state.value.email)
        assertEquals("Password123!", viewModel.state.value.password)
        assertTrue(viewModel.state.value.isSignupEnabled)
    }

    @Test
    fun `given back click event then emits NavigateBack effect`() = runTest(testDispatcher) {
        val effects = mutableListOf<Effect>()
        val viewModel = createViewModel()
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects.add(it) }
        }

        viewModel.sendEvent(Event.OnBackClicked)
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(Effect.NavigateBack, effects.first())
    }

    @Test
    fun `given successful signup for free user when signup clicked then navigates to WelcomeDiscovery`() = runTest(testDispatcher) {
        coEvery { validateCredentialsUseCase(any()) } returns Result.success(
            ValidateCredentialsUseCase.Output(isEmailValid = true, isPasswordValid = true, canLogin = true)
        )
        every { evaluateIdentityConflictUseCase(any()) } returns flowOf(
            EvaluateIdentityConflictUseCase.Output.NoConflict
        )
        val testUser = User(id = "u2", email = "new@example.com", name = "New User")
        every { signUpUseCase(any()) } returns flowOf(
            SignUpUseCase.Output.Success(testUser)
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

        viewModel.sendEvent(Event.OnNameChanged("New User"))
        viewModel.sendEvent(Event.OnEmailChanged("new@example.com"))
        viewModel.sendEvent(Event.OnPasswordChanged("pass123"))
        viewModel.sendEvent(Event.OnSignupClicked)
        advanceUntilIdle()

        assertEquals(1, effects.size)
        assertEquals(Effect.NavigateToWelcomeDiscovery, effects.first())
    }

    @Test
    fun `given signup failure when signup clicked then shows error in state`() = runTest(testDispatcher) {
        every { evaluateIdentityConflictUseCase(any()) } returns flowOf(
            EvaluateIdentityConflictUseCase.Output.NoConflict
        )
        every { signUpUseCase(any()) } returns flowOf(
            SignUpUseCase.Output.Failure(KmError.UserAlreadyRegistered)
        )

        val viewModel = createViewModel()
        viewModel.sendEvent(Event.OnNameChanged("Existing User"))
        viewModel.sendEvent(Event.OnEmailChanged("existing@example.com"))
        viewModel.sendEvent(Event.OnPasswordChanged("pass123"))
        viewModel.sendEvent(Event.OnSignupClicked)
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }
}
