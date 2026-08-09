package es.joshluq.kmsafe.ui.profile

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.di.DeleteAccount
import es.joshluq.kmsafe.di.GetCurrentUser
import es.joshluq.kmsafe.di.GetEntitlements
import es.joshluq.kmsafe.di.SignOut
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.usecase.DeleteAccountUseCase
import es.joshluq.kmsafe.domain.usecase.GetCurrentUserUseCase
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @param:SignOut private val signOutUseCase:
    @JvmSuppressWildcards FlowUseCase<SignOutUseCase.Input, SignOutUseCase.Output>,
    @param:GetCurrentUser private val getCurrentUserUseCase:
    @JvmSuppressWildcards FlowUseCase<GetCurrentUserUseCase.Input, GetCurrentUserUseCase.Output>,
    @param:DeleteAccount private val deleteAccountUseCase:
    @JvmSuppressWildcards FlowUseCase<DeleteAccountUseCase.Input, DeleteAccountUseCase.Output>,
    @param:GetEntitlements private val getEntitlementsUseCase:
    @JvmSuppressWildcards FlowUseCase<GetEntitlementsUseCase.Input, GetEntitlementsUseCase.Output>,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    init {
        observeUser()
        observeEntitlements()
    }

    override fun createInitialState(): State = State()

    override fun handleEvent(event: Event) {
        logger.d("ProfileViewModel", "Event received: $event")
        when (event) {
            Event.OnVehiclesClicked -> {
                logger.d("ProfileViewModel", "Effect launched: NavigateToVehicles")
                launchEffect(Effect.NavigateToVehicles)
            }
            Event.OnDataManagementClicked -> {
                logger.d("ProfileViewModel", "Effect launched: NavigateToDataManagement")
                launchEffect(Effect.NavigateToDataManagement)
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
            Event.OnDismissError -> updateState { copy(error = null) }
        }
    }

    private fun observeUser() {
        getCurrentUserUseCase(GetCurrentUserUseCase.Input)
            .onEach { output ->
                if (output is GetCurrentUserUseCase.Output.Success) {
                    updateState { copy(user = output.user) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeEntitlements() {
        getEntitlementsUseCase(GetEntitlementsUseCase.Input("", forceRefresh = false))
            .onEach { output ->
                if (output is GetEntitlementsUseCase.Output.Success) {
                    updateState { copy(entitlements = output.entitlements) }
                }
            }.launchIn(viewModelScope)
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
                    logger.i("ProfileViewModel", "Logout success, navigating to Login")
                    launchEffect(Effect.NavigateToLogin)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun handleDeleteAccount() {
        deleteAccountUseCase(DeleteAccountUseCase.Input).onEach { output ->
            when (output) {
                DeleteAccountUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                is DeleteAccountUseCase.Output.Failure -> {
                    updateState {
                        copy(
                            isLoading = false,
                            error = TextProvider.Resource(R.string.profile_delete_account_error)
                        )
                    }
                }
                DeleteAccountUseCase.Output.Success -> {
                    logger.i("ProfileViewModel", "Account deletion success, navigating to Login")
                    launchEffect(Effect.NavigateToLogin)
                }
            }
        }.launchIn(viewModelScope)
    }
}
