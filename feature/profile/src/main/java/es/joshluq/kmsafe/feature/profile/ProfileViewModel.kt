package es.joshluq.kmsafe.feature.profile

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.AppOverlayState
import es.joshluq.kmsafe.domain.usecase.DeleteAccountUseCase
import es.joshluq.kmsafe.domain.usecase.GetCurrentUserUseCase
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.SetAppOverlayUseCase
import es.joshluq.kmsafe.domain.usecase.SignOutUseCase
import es.joshluq.kmsafe.feature.profile.domain.ProfileConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val signOutUseCase: SignOutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val getEntitlementsUseCase: GetEntitlementsUseCase,
    private val setAppOverlayUseCase: SetAppOverlayUseCase,
    private val profileConfig: ProfileConfig,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    private var userJob: Job? = null
    private var entitlementsJob: Job? = null

    init {
        observeUser()
        observeEntitlements()
        updateState { 
            copy(
                termsUrl = profileConfig.getTermsUrl(),
                privacyUrl = profileConfig.getPrivacyUrl()
            )
        }
    }

    override fun createInitialState(): State = State()

    override fun handleEvent(event: Event) {
        logger.d("ProfileViewModel", "Event received: $event")
        when (event) {
            Event.OnResume -> {
                observeUser()
                observeEntitlements()
            }
            Event.OnVehiclesClicked -> {
                logger.d("ProfileViewModel", "Effect launched: NavigateToVehicles")
                launchEffect(Effect.NavigateToVehicles)
            }
            Event.OnPreferencesClicked -> {
                logger.d("ProfileViewModel", "Effect launched: NavigateToPreferences")
                launchEffect(Effect.NavigateToPreferences)
            }
            Event.OnLogoutClicked -> updateState { copy(showLogoutConfirmation = true) }
            Event.OnLogoutConfirmed -> {
                updateState { copy(showLogoutConfirmation = false) }
                handleLogout()
            }
            Event.OnLogoutCancelled -> updateState { copy(showLogoutConfirmation = false) }
            Event.OnUpgradeClicked -> {
                logger.d("ProfileViewModel", "Effect launched: NavigateToPremiumPaywall")
                launchEffect(Effect.NavigateToPremiumPaywall)
            }
            Event.OnDeleteAccountClicked -> updateState { copy(showDeleteConfirmation = true) }
            Event.OnDeleteAccountConfirmed -> {
                updateState { copy(showDeleteConfirmation = false) }
                handleDeleteAccount()
            }
            Event.OnDeleteAccountCancelled -> updateState { copy(showDeleteConfirmation = false) }
            Event.OnWelcomeGuideClicked -> {
                logger.d("ProfileViewModel", "Effect launched: NavigateToWelcomeDiscovery")
                launchEffect(Effect.NavigateToWelcomeDiscovery)
            }
            Event.OnDismissError -> updateState { copy(error = null) }
        }
    }

    private fun observeUser() {
        userJob?.cancel()
        userJob = getCurrentUserUseCase(GetCurrentUserUseCase.Input)
            .onEach { output ->
                if (output is GetCurrentUserUseCase.Output.Success) {
                    updateState { copy(user = output.user) }
                }
            }
            .catch { logger.e("ProfileViewModel", "Error observing user", it) }
            .launchIn(viewModelScope)
    }

    private fun observeEntitlements() {
        entitlementsJob?.cancel()
        entitlementsJob = getEntitlementsUseCase(GetEntitlementsUseCase.Input("", forceRefresh = false))
            .onEach { output ->
                if (output is GetEntitlementsUseCase.Output.Success) {
                    updateState { copy(entitlements = output.entitlements) }
                }
            }
            .catch { logger.e("ProfileViewModel", "Error observing entitlements", it) }
            .launchIn(viewModelScope)
    }

    private fun handleLogout() {
        val entitlements = state.value.entitlements
        val clearData = entitlements?.subscriptionLevel == SubscriptionLevel.PREMIUM ||
            entitlements?.subscriptionLevel == SubscriptionLevel.TRIAL

        signOutUseCase(SignOutUseCase.Input(clearLocalData = clearData)).onEach { output ->
            when (output) {
                SignOutUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                is SignOutUseCase.Output.Failure -> {
                    updateState { copy(isLoading = false) }
                    logger.e("ProfileViewModel", "Logout failed: ${output.message}")
                }
                SignOutUseCase.Output.Success -> {
                    logger.i("ProfileViewModel", "Logout success, resetting state and navigating to Login")
                    updateState {
                        createInitialState().copy(
                            termsUrl = profileConfig.getTermsUrl(),
                            privacyUrl = profileConfig.getPrivacyUrl()
                        )
                    }
                    launchEffect(Effect.NavigateToLogin)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun handleDeleteAccount() {
        deleteAccountUseCase(DeleteAccountUseCase.Input).onEach { output ->
            when (output) {
                DeleteAccountUseCase.Output.Progress -> {
                    val msg = TextProvider.Resource(R.string.profile_delete_account_step_cleaning)
                    updateState { copy(isDeleting = true, deletionMessage = msg) }
                    setAppOverlayUseCase(SetAppOverlayUseCase.Input(AppOverlayState.AccountDeletion(stepMessage = msg, progress = 0.15f))).launchIn(viewModelScope)
                }
                is DeleteAccountUseCase.Output.Failure -> {
                    setAppOverlayUseCase(SetAppOverlayUseCase.Input(AppOverlayState.None)).launchIn(viewModelScope)
                    updateState {
                        copy(
                            isDeleting = false,
                            deletionMessage = null,
                            error = TextProvider.Resource(R.string.profile_delete_account_error)
                        )
                    }
                }
                DeleteAccountUseCase.Output.Success -> {
                    logger.i("ProfileViewModel", "Account deletion success, showing farewell messages")
                    
                    val msg1 = TextProvider.Resource(R.string.profile_delete_account_step_cloud)
                    updateState { copy(deletionMessage = msg1) }
                    setAppOverlayUseCase(SetAppOverlayUseCase.Input(AppOverlayState.AccountDeletion(stepMessage = msg1, progress = 0.40f))).launchIn(viewModelScope)
                    delay(1500.milliseconds)
                    
                    val msg2 = TextProvider.Resource(R.string.profile_delete_account_step_farewell)
                    updateState { copy(deletionMessage = msg2) }
                    setAppOverlayUseCase(SetAppOverlayUseCase.Input(AppOverlayState.AccountDeletion(stepMessage = msg2, progress = 0.70f))).launchIn(viewModelScope)
                    delay(2000.milliseconds)
                    
                    val msg3 = TextProvider.Resource(R.string.profile_delete_account_step_final)
                    updateState { copy(deletionMessage = msg3) }
                    setAppOverlayUseCase(SetAppOverlayUseCase.Input(AppOverlayState.AccountDeletion(stepMessage = msg3, progress = 0.90f))).launchIn(viewModelScope)
                    delay(1500.milliseconds)
                    
                    val msg4 = TextProvider.Resource(R.string.profile_delete_account_step_welcome_back)
                    updateState { copy(deletionMessage = msg4) }
                    setAppOverlayUseCase(SetAppOverlayUseCase.Input(AppOverlayState.AccountDeletion(stepMessage = msg4, progress = 1.0f))).launchIn(viewModelScope)
                    delay(2000.milliseconds)

                    setAppOverlayUseCase(SetAppOverlayUseCase.Input(AppOverlayState.None)).launchIn(viewModelScope)
                    updateState {
                        createInitialState().copy(
                            termsUrl = profileConfig.getTermsUrl(),
                            privacyUrl = profileConfig.getPrivacyUrl()
                        )
                    }
                    launchEffect(Effect.NavigateToLogin)
                }
            }
        }.launchIn(viewModelScope)
    }
}
