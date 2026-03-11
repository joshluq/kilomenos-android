package es.joshluq.kmsafe.ui.signup

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
import es.joshluq.kmsafe.di.ClearLocalData
import es.joshluq.kmsafe.di.EvaluateIdentityConflict
import es.joshluq.kmsafe.di.SignUp
import es.joshluq.kmsafe.di.UpdatePreferences
import es.joshluq.kmsafe.di.ValidateCredentials
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.usecase.ClearLocalDataUseCase
import es.joshluq.kmsafe.domain.usecase.EvaluateIdentityConflictUseCase
import es.joshluq.kmsafe.domain.usecase.SignUpUseCase
import es.joshluq.kmsafe.domain.usecase.UpdatePreferencesUseCase
import es.joshluq.kmsafe.domain.usecase.ValidateCredentialsUseCase
import es.joshluq.kmsafe.ui.util.toText
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    @param:ValidateCredentials private val validateCredentialsUseCase:
    @JvmSuppressWildcards UseCase<ValidateCredentialsUseCase.Input, ValidateCredentialsUseCase.Output>,
    @param:SignUp private val signUpUseCase:
    @JvmSuppressWildcards FlowUseCase<SignUpUseCase.Input, SignUpUseCase.Output>,
    @param:ClearLocalData private val clearLocalDataUseCase:
    @JvmSuppressWildcards FlowUseCase<ClearLocalDataUseCase.Input, ClearLocalDataUseCase.Output>,
    @param:EvaluateIdentityConflict private val evaluateIdentityConflictUseCase:
    @JvmSuppressWildcards FlowUseCase<EvaluateIdentityConflictUseCase.Input, EvaluateIdentityConflictUseCase.Output>,
    @param:UpdatePreferences private val updatePreferencesUseCase:
    @JvmSuppressWildcards FlowUseCase<UpdatePreferencesUseCase.Input, UpdatePreferencesUseCase.Output>,
    private val analytics: AnalyticskitManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    private var shouldClearDataOnSuccess = false

    init {
        analytics.track(AnalyticsEvent.FunnelStep("signup", "started"))
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
                        analytics.track(AnalyticsEvent.Custom("user_conflict_alert_shown"))
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
                    updateState { copy(isLoading = false, error = output.error.toText()) }
                }
                is SignUpUseCase.Output.Success -> {
                    analytics.track(AnalyticsEvent.FunnelStep("signup", "completed"))
                    saveEmailPreference(output.user.email)

                    if (shouldClearDataOnSuccess) {
                        clearLocalDataAndProceed(output.user)
                    } else {
                        handlePostSignupNavigation(output.user)
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun clearLocalDataAndProceed(user: User) {
        clearLocalDataUseCase(ClearLocalDataUseCase.Input).onEach { output ->
            if (output is ClearLocalDataUseCase.Output.Success) {
                handlePostSignupNavigation(user)
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

    private fun handlePostSignupNavigation(user: User) {
        if (user.subscriptionLevel == SubscriptionLevel.PREMIUM) {
            launchEffect(Effect.NavigateToDashboard)
        } else {
            launchEffect(Effect.NavigateToPremiumPaywall)
        }
    }
}
