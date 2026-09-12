package es.joshluq.kmsafe.feature.auth.signup

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.kmsafe.core.analytics.AnalyticsTracker
import es.joshluq.kmsafe.core.analytics.model.KmsafeAnalyticsEvent
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.feature.auth.R
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.usecase.ClearLocalDataUseCase
import es.joshluq.kmsafe.domain.usecase.EvaluateIdentityConflictUseCase
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.SignUpUseCase
import es.joshluq.kmsafe.domain.usecase.SyncContractsUseCase
import es.joshluq.kmsafe.domain.usecase.UpdatePreferencesUseCase
import es.joshluq.kmsafe.domain.usecase.ValidateCredentialsUseCase
import es.joshluq.kmsafe.core.ui.util.toText
import kotlinx.coroutines.flow.launchIn
import es.joshluq.kmsafe.feature.auth.domain.AuthConfig
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val validateCredentialsUseCase: ValidateCredentialsUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val clearLocalDataUseCase: ClearLocalDataUseCase,
    private val evaluateIdentityConflictUseCase: EvaluateIdentityConflictUseCase,
    private val updatePreferencesUseCase: UpdatePreferencesUseCase,
    private val getEntitlementsUseCase: GetEntitlementsUseCase,
    private val syncContractsUseCase: SyncContractsUseCase,
    private val authConfig: AuthConfig,
    private val analytics: AnalyticsTracker,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    private var shouldClearDataOnSuccess = false

    init {
        analytics.track(KmsafeAnalyticsEvent.Auth.SignUpStarted())
        updateState { 
            copy(
                termsUrl = authConfig.getTermsUrl(),
                privacyUrl = authConfig.getPrivacyUrl()
            )
        }
    }

    override fun createInitialState(): State = State.Empty

    override fun handleEvent(event: Event) {
        logger.d("SignupViewModel", "Event received: $event")
        when (event) {
            is Event.OnNameChanged -> updateState { copy(name = event.value) }
            is Event.OnEmailChanged -> handleEmailChanged(event.value)
            is Event.OnPasswordChanged -> handlePasswordChanged(event.value)
            Event.OnTogglePasswordVisibility -> updateState { copy(isPasswordVisible = !isPasswordVisible) }
            Event.OnSignupClicked -> handleSignupClicked()
            Event.OnConfirmUserConflict -> handleConfirmUserConflict()
            Event.OnDismissUserConflict -> updateState { copy(showUserConflictWarning = false, isLoading = false) }
            Event.OnDismissError -> updateState { copy(error = null) }
            Event.OnBackClicked -> launchEffect(Effect.NavigateBack)
        }
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
                        isSignupEnabled = validation.canLogin && name.isNotBlank()
                    )
                }
            }
        }
    }

    private fun handlePasswordChanged(value: String) {
        viewModelScope.launch {
            validateCredentialsUseCase(
                ValidateCredentialsUseCase.Input(state.value.email, value)
            ).onSuccess { validation ->
                updateState {
                    copy(
                        password = value,
                        passwordError = if (value.isNotEmpty() && !validation.isPasswordValid) {
                            TextProvider.Resource(R.string.login_password_error)
                        } else {
                            null
                        },
                        isSignupEnabled = validation.canLogin && name.isNotBlank()
                    )
                }
            }
        }
    }

    private fun handleSignupClicked() {
        evaluateIdentityConflictUseCase(EvaluateIdentityConflictUseCase.Input(state.value.email))
            .onEach { output ->
                when (output) {
                    EvaluateIdentityConflictUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                    EvaluateIdentityConflictUseCase.Output.NoConflict -> {
                        shouldClearDataOnSuccess = false
                        performSignup()
                    }
                    EvaluateIdentityConflictUseCase.Output.SilentCleanup -> {
                        shouldClearDataOnSuccess = true
                        performSignup()
                    }
                    EvaluateIdentityConflictUseCase.Output.ShowWarning -> {
                        analytics.track(KmsafeAnalyticsEvent.Auth.UserConflictAlertShown)
                        updateState { copy(showUserConflictWarning = true, isLoading = false) }
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun handleConfirmUserConflict() {
        shouldClearDataOnSuccess = true
        updateState { copy(showUserConflictWarning = false) }
        performSignup()
    }

    private fun performSignup() {
        val input = SignUpUseCase.Input(
            email = state.value.email,
            password = state.value.password,
            name = state.value.name
        )
        signUpUseCase(input).onEach { output ->
            when (output) {
                SignUpUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                is SignUpUseCase.Output.Failure -> {
                    analytics.track(KmsafeAnalyticsEvent.Auth.SignUpFailed(output.error.toString()))
                    updateState { copy(isLoading = false, error = output.error.toText()) }
                }
                is SignUpUseCase.Output.Success -> {
                    analytics.track(KmsafeAnalyticsEvent.Auth.SignUpCompleted())
                    saveEmailPreference(output.user.email)

                    if (shouldClearDataOnSuccess) {
                        clearLocalDataAndProceed()
                    } else {
                        proceedWithPostSignup()
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun clearLocalDataAndProceed() {
        clearLocalDataUseCase(ClearLocalDataUseCase.Input).onEach { output ->
            if (output is ClearLocalDataUseCase.Output.Success) {
                proceedWithPostSignup()
            }
        }.launchIn(viewModelScope)
    }

    private fun proceedWithPostSignup() {
        logger.d("SignupViewModel", "Proceeding with mandatory sync before navigation")
        updateState { copy(isLoading = true) }

        // 1. Fetch Entitlements FIRST (to seed the session properly)
        getEntitlementsUseCase(GetEntitlementsUseCase.Input("", forceRefresh = true))
            .onEach { output ->
                when (output) {
                    is GetEntitlementsUseCase.Output.Success -> {
                        logger.i("SignupViewModel", "Entitlements synced. Starting contract sync.")
                        syncContracts(output.entitlements.subscriptionLevel)
                    }
                    is GetEntitlementsUseCase.Output.Failure -> {
                        logger.w("SignupViewModel", "Entitlements sync failed. Proceeding as FREE.")
                        syncContracts(SubscriptionLevel.FREE)
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun syncContracts(level: SubscriptionLevel) {
        syncContractsUseCase(SyncContractsUseCase.Input).onEach { syncOutput ->
            when (syncOutput) {
                SyncContractsUseCase.Output.Progress -> Unit
                is SyncContractsUseCase.Output.Failure,
                SyncContractsUseCase.Output.Success -> {
                    logger.i("SignupViewModel", "Post-signup sync complete. Navigating.")
                    updateState { copy(isLoading = false) }
                    handlePostSignupNavigation(level)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun saveEmailPreference(email: String) {
        updatePreferencesUseCase(
            UpdatePreferencesUseCase.Input(
                lastEmail = email.trim().lowercase(),
                rememberEmail = null
            )
        ).launchIn(viewModelScope)
    }

    private fun handlePostSignupNavigation(level: SubscriptionLevel) {
        if (level == SubscriptionLevel.PREMIUM) {
            launchEffect(Effect.NavigateToDashboard)
        } else {
            launchEffect(Effect.NavigateToWelcomeDiscovery)
        }
    }
}
