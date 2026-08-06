package es.joshluq.kmsafe.ui.login

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.data.util.DeviceFingerprintProvider
import es.joshluq.kmsafe.di.*
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.usecase.*
import es.joshluq.kmsafe.data.remote.auth.GoogleAuthManager
import es.joshluq.kmsafe.ui.util.toText
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Login screen, managing credential validation and authentication flows.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    @param:ValidateCredentials private val validateCredentialsUseCase:
    @JvmSuppressWildcards UseCase<ValidateCredentialsUseCase.Input, ValidateCredentialsUseCase.Output>,
    @param:SignIn private val signInUseCase:
    @JvmSuppressWildcards FlowUseCase<SignInUseCase.Input, SignInUseCase.Output>,
    @param:SignInWithGoogle private val signInWithGoogleUseCase:
    @JvmSuppressWildcards FlowUseCase<SignInWithGoogleUseCase.Input, SignInWithGoogleUseCase.Output>,
    @param:SyncContracts private val syncContractsUseCase:
    @JvmSuppressWildcards FlowUseCase<SyncContractsUseCase.Input, SyncContractsUseCase.Output>,
    @param:ClearLocalData private val clearLocalDataUseCase:
    @JvmSuppressWildcards FlowUseCase<ClearLocalDataUseCase.Input, ClearLocalDataUseCase.Output>,
    @param:EvaluateIdentityConflict private val evaluateIdentityConflictUseCase:
    @JvmSuppressWildcards FlowUseCase<EvaluateIdentityConflictUseCase.Input, EvaluateIdentityConflictUseCase.Output>,
    @param:GetPreferences private val getPreferencesUseCase:
    @JvmSuppressWildcards FlowUseCase<GetPreferencesUseCase.Input, GetPreferencesUseCase.Output>,
    @param:UpdatePreferences private val updatePreferencesUseCase:
    @JvmSuppressWildcards FlowUseCase<UpdatePreferencesUseCase.Input, UpdatePreferencesUseCase.Output>,
    @param:GetEntitlements private val getEntitlementsUseCase:
    @JvmSuppressWildcards FlowUseCase<GetEntitlementsUseCase.Input, GetEntitlementsUseCase.Output>,
    private val fingerprintProvider: DeviceFingerprintProvider,
    private val googleAuthManager: GoogleAuthManager,
    private val analytics: AnalyticskitManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    private var shouldClearDataOnSuccess = false

    init {
        analytics.track(AnalyticsEvent.Custom("login_started"))
        loadPreferences()
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
                isLoginEnabled = emailError == null && value.isNotEmpty()
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
            googleAuthManager.signIn(context)?.let { idToken ->
                performGoogleLogin(idToken)
            }
        }
    }

    private fun handleConfirmUserConflict() {
        shouldClearDataOnSuccess = true
        updateState { copy(showUserConflictWarning = false) }
        performLogin()
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
                    analytics.track(AnalyticsEvent.Custom("login_google_failure", mapOf("error" to output.error.toString())))
                    updateState { copy(isLoading = false, error = output.error.toText()) }
                }
                is SignInWithGoogleUseCase.Output.Success -> {
                    handleAuthSuccess(output.user)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun handleAuthSuccess(user: User) {
        logger.d("LoginViewModel", "Auth success for: ${user.email}. shouldClearDataOnSuccess: $shouldClearDataOnSuccess")
        saveEmailPreference(user.email)

        if (shouldClearDataOnSuccess) {
            clearLocalDataAndProceed(user)
        } else {
            proceedWithPostLogin(user)
        }
    }

    private fun clearLocalDataAndProceed(user: User) {
        clearLocalDataUseCase(ClearLocalDataUseCase.Input).onEach { output ->
            if (output is ClearLocalDataUseCase.Output.Success) {
                proceedWithPostLogin(user)
            }
        }.launchIn(viewModelScope)
    }

    private fun saveEmailPreference(email: String) {
        updatePreferencesUseCase(
            UpdatePreferencesUseCase.Input(
                lastEmail = email.trim().lowercase(),
                rememberEmail = true
            )
        ).launchIn(viewModelScope)
    }

    private fun proceedWithPostLogin(user: User) {
        logger.d("LoginViewModel", "Proceeding with mandatory sync before navigation")
        updateState { copy(isLoading = true) }

        val fingerprint = fingerprintProvider.getFingerprint()
        val contractsFlow = syncContractsUseCase(SyncContractsUseCase.Input)
        val entitlementsFlow = getEntitlementsUseCase(GetEntitlementsUseCase.Input(fingerprint, forceRefresh = true))

        combine(contractsFlow, entitlementsFlow) { contracts, entitlements ->
            val isContractsDone = contracts is SyncContractsUseCase.Output.Success || 
                contracts is SyncContractsUseCase.Output.Failure
            val isEntitlementsDone = entitlements is GetEntitlementsUseCase.Output.Success || 
                entitlements is GetEntitlementsUseCase.Output.Failure
            
            isContractsDone && isEntitlementsDone
        }.onEach { isBothDone ->
            if (isBothDone) {
                logger.i("LoginViewModel", "Post-login sync success, navigating based on level")
                updateState { copy(isLoading = false) }
                handlePostLoginNavigation(user)
            }
        }.launchIn(viewModelScope)
    }

    private fun handlePostLoginNavigation(user: User) {
        if (user.subscriptionLevel == SubscriptionLevel.PREMIUM) {
            launchEffect(Effect.NavigateToDashboard)
        } else {
            launchEffect(Effect.NavigateToPremiumPaywall)
        }
    }
}
