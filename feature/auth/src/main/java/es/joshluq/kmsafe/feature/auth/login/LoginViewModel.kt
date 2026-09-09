package es.joshluq.kmsafe.feature.auth.login

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.feature.auth.R
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.User
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
import es.joshluq.kmsafe.core.ui.util.toText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import es.joshluq.kmsafe.feature.auth.domain.AuthConfig
import javax.inject.Inject

/**
 * ViewModel for the Login screen, managing credential validation and authentication flows.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val validateCredentialsUseCase: ValidateCredentialsUseCase,
    private val signInUseCase: SignInUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val syncContractsUseCase: SyncContractsUseCase,
    private val clearLocalDataUseCase: ClearLocalDataUseCase,
    private val evaluateIdentityConflictUseCase: EvaluateIdentityConflictUseCase,
    private val getPreferencesUseCase: GetPreferencesUseCase,
    private val updatePreferencesUseCase: UpdatePreferencesUseCase,
    private val getEntitlementsUseCase: GetEntitlementsUseCase,
    private val fingerprintProvider: FingerprintProvider,
    private val socialAuthService: SocialAuthService,
    private val authConfig: AuthConfig,
    private val analytics: AnalyticskitManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    private var shouldClearDataOnSuccess = false
    private var pendingUser: User? = null

    init {
        analytics.track(AnalyticsEvent.Custom("login_started"))
        loadPreferences()
        updateState { 
            copy(
                termsUrl = authConfig.getTermsUrl(),
                privacyUrl = authConfig.getPrivacyUrl()
            )
        }
    }

    override fun createInitialState(): State = State.Empty

    override fun handleEvent(event: Event) {
        logger.d("LoginViewModel", "Event received: $event")
        when (event) {
            is Event.OnEmailChanged -> handleEmailChanged(event.value)
            is Event.OnPasswordChanged -> handlePasswordChanged(event.value)
            Event.OnTogglePasswordVisibility -> updateState { copy(isPasswordVisible = !isPasswordVisible) }
            Event.OnLoginClicked -> handleLoginClicked()
            Event.OnGoogleSignInClicked -> handleGoogleSignInClicked()
            Event.OnConfirmUserConflict -> handleConfirmUserConflict()
            Event.OnDismissUserConflict -> updateState {
                copy(
                    showUserConflictWarning = false,
                    pendingUser = null,
                    isLoading = false
                )
            }
            Event.OnDismissError -> updateState { copy(error = null) }
        }
    }

    private fun loadPreferences() {
        getPreferencesUseCase(GetPreferencesUseCase.Input)
            .onEach { output ->
                if (output is GetPreferencesUseCase.Output.Success) {
                    if (output.preferences.rememberEmail && output.preferences.lastEmail.isNotEmpty()) {
                        handleEmailChanged(output.preferences.lastEmail)
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun handleEmailChanged(value: String) {
        viewModelScope.launch {
            validateCredentialsUseCase(
                ValidateCredentialsUseCase.Input(value, state.value.password)
            ).onSuccess { validation ->
                updateState {
                    copy(
                        email = value,
                        emailError = if (value.isNotEmpty() && !validation.isEmailValid) {
                            TextProvider.Resource(R.string.login_email_error)
                        } else {
                            null
                        },
                        isLoginEnabled = validation.canLogin
                    )
                }
            }
        }
    }

    private fun handlePasswordChanged(value: String) {
        updateState {
            copy(
                password = value,
                isLoginEnabled = emailError == null && email.isNotEmpty() && value.isNotEmpty()
            )
        }
    }

    private fun handleLoginClicked() {
        evaluateIdentityConflictUseCase(EvaluateIdentityConflictUseCase.Input(state.value.email))
            .onEach { output ->
                when (output) {
                    EvaluateIdentityConflictUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                    EvaluateIdentityConflictUseCase.Output.NoConflict -> {
                        shouldClearDataOnSuccess = false
                        performLogin()
                    }
                    EvaluateIdentityConflictUseCase.Output.SilentCleanup -> {
                        shouldClearDataOnSuccess = true
                        performLogin()
                    }
                    EvaluateIdentityConflictUseCase.Output.ShowWarning -> {
                        analytics.track(AnalyticsEvent.Custom("user_conflict_alert_shown"))
                        updateState { copy(showUserConflictWarning = true, isLoading = false) }
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun handleGoogleSignInClicked() {
        analytics.track(AnalyticsEvent.Custom("login_google_started"))
        launchEffect(Effect.TriggerGoogleSignIn)
    }

    fun triggerGoogleSignIn(context: Context) {
        viewModelScope.launch {
            socialAuthService.signIn(context)?.let { idToken ->
                performGoogleLogin(idToken)
            }
        }
    }

    private fun handleConfirmUserConflict() {
        shouldClearDataOnSuccess = true
        updateState { copy(showUserConflictWarning = false) }
        
        val user = pendingUser
        if (user != null) {
            // Continuation for Google Login
            handleAuthSuccess(user)
            pendingUser = null
        } else {
            // Continuation for Email Login
            performLogin()
        }
    }

    private fun performLogin() {
        val input = SignInUseCase.Input(state.value.email, state.value.password)
        signInUseCase(input).onEach { output ->
            when (output) {
                SignInUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                is SignInUseCase.Output.Failure -> {
                    analytics.track(AnalyticsEvent.Custom("login_failure", mapOf("error" to output.error.toString())))
                    updateState { copy(isLoading = false, error = output.error.toText()) }
                }

                is SignInUseCase.Output.Success -> {
                    handleAuthSuccess(output.user)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun performGoogleLogin(idToken: String) {
        signInWithGoogleUseCase(SignInWithGoogleUseCase.Input(idToken)).onEach { output ->
            when (output) {
                SignInWithGoogleUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                is SignInWithGoogleUseCase.Output.Failure -> {
                    analytics.track(
                        AnalyticsEvent.Custom("login_google_failure", mapOf("error" to output.error.toString()))
                    )
                    updateState { copy(isLoading = false, error = output.error.toText()) }
                }
                is SignInWithGoogleUseCase.Output.Success -> {
                    evaluateIdentityConflictUseCase(EvaluateIdentityConflictUseCase.Input(output.user.email))
                        .onEach { conflictOutput ->
                            when (conflictOutput) {
                                EvaluateIdentityConflictUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                                EvaluateIdentityConflictUseCase.Output.NoConflict -> {
                                    shouldClearDataOnSuccess = false
                                    handleAuthSuccess(output.user)
                                }
                                EvaluateIdentityConflictUseCase.Output.SilentCleanup -> {
                                    shouldClearDataOnSuccess = true
                                    handleAuthSuccess(output.user)
                                }
                                EvaluateIdentityConflictUseCase.Output.ShowWarning -> {
                                    analytics.track(AnalyticsEvent.Custom("user_conflict_alert_shown"))
                                    pendingUser = output.user
                                    updateState { copy(showUserConflictWarning = true, isLoading = false) }
                                }
                            }
                        }.launchIn(viewModelScope)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun handleAuthSuccess(user: User) {
        logger.d(
            "LoginViewModel",
            "Auth success for: ${user.email}. shouldClearDataOnSuccess: $shouldClearDataOnSuccess"
        )
        saveEmailPreference(user.email)

        if (shouldClearDataOnSuccess) {
            clearLocalDataAndProceed()
        } else {
            proceedWithPostLogin()
        }
    }

    private fun clearLocalDataAndProceed() {
        viewModelScope.launch {
            try {
                clearLocalDataUseCase(ClearLocalDataUseCase.Input)
                    .first { it !is ClearLocalDataUseCase.Output.Progress }
                logger.i("LoginViewModel", "Local data cleared successfully.")
            } catch (e: Exception) {
                logger.e("LoginViewModel", "Clear local data failed: ${e.message}")
            }
            proceedWithPostLogin()
        }
    }

    private fun saveEmailPreference(email: String) {
        updatePreferencesUseCase(
            UpdatePreferencesUseCase.Input(
                lastEmail = email.trim().lowercase(),
                rememberEmail = true
            )
        ).launchIn(viewModelScope)
    }

    private fun proceedWithPostLogin() {
        logger.d("LoginViewModel", "Proceeding with mandatory sync before navigation")
        updateState { copy(isLoading = true) }

        viewModelScope.launch {
            val fingerprint = fingerprintProvider.getFingerprint()

            // 1. Fetch Entitlements FIRST (Linearized)
            var subscriptionLevel = SubscriptionLevel.FREE
            try {
                val entitlementsOutput = getEntitlementsUseCase(
                    GetEntitlementsUseCase.Input(fingerprint, forceRefresh = true)
                ).first()

                if (entitlementsOutput is GetEntitlementsUseCase.Output.Success) {
                    subscriptionLevel = entitlementsOutput.entitlements.subscriptionLevel
                    logger.i("LoginViewModel", "Entitlements synced. Level: $subscriptionLevel")
                } else {
                    logger.w("LoginViewModel", "Entitlements sync output not Success. Proceeding as FREE.")
                }
            } catch (e: Exception) {
                logger.e("LoginViewModel", "Entitlements sync failed: ${e.message}. Proceeding as FREE.")
            }

            // 2. Fetch Contracts, Stations, History, and Fuel Expenses SECOND (Linearized)
            try {
                syncContractsUseCase(SyncContractsUseCase.Input)
                    .first { it !is SyncContractsUseCase.Output.Progress }
                logger.i("LoginViewModel", "Initial contract and expense data sync complete.")
            } catch (e: Exception) {
                logger.e("LoginViewModel", "Contract sync failed: ${e.message}")
            }

            // 3. Grace delay for DB settlement & safe navigation
            delay(500.milliseconds)
            updateState { copy(isLoading = false) }
            handlePostLoginNavigation(subscriptionLevel)
        }
    }

    private fun handlePostLoginNavigation(level: SubscriptionLevel) {
        if (level == SubscriptionLevel.PREMIUM) {
            launchEffect(Effect.NavigateToDashboard)
        } else {
            launchEffect(Effect.NavigateToPremiumPaywall)
        }
    }
}
