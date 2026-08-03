package es.joshluq.kmsafe.ui.profile.preferences

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.data.location.AutoTrackingManager
import es.joshluq.kmsafe.di.GetPreferences
import es.joshluq.kmsafe.di.IsUserPremium
import es.joshluq.kmsafe.di.UpdatePreferences
import es.joshluq.kmsafe.domain.usecase.GetPreferencesUseCase
import es.joshluq.kmsafe.domain.usecase.IsUserPremiumUseCase
import es.joshluq.kmsafe.domain.usecase.UpdatePreferencesUseCase
import es.joshluq.kmsafe.ui.util.ConsentManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class PreferencesViewModel @Inject constructor(
    @param:GetPreferences private val getPreferencesUseCase:
    @JvmSuppressWildcards FlowUseCase<GetPreferencesUseCase.Input, GetPreferencesUseCase.Output>,
    @param:UpdatePreferences private val updatePreferencesUseCase:
    @JvmSuppressWildcards FlowUseCase<UpdatePreferencesUseCase.Input, UpdatePreferencesUseCase.Output>,
    @param:IsUserPremium private val isUserPremiumUseCase:
    @JvmSuppressWildcards FlowUseCase<IsUserPremiumUseCase.Input, IsUserPremiumUseCase.Output>,
    private val autoTrackingManager: AutoTrackingManager,
    private val consentManager: ConsentManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    init {
        checkPremiumStatus()
        loadPreferences()
        updateState { copy(isPrivacyOptionsRequired = consentManager.isPrivacyOptionsRequired()) }
    }

    override fun createInitialState(): State = State.Empty

    override fun handleEvent(event: Event) {
        logger.d("PreferencesViewModel", "Event received: $event")
        when (event) {
            is Event.OnRememberEmailToggled -> handleRememberEmailToggled(event.enabled)
            is Event.OnProjectionBannerToggled -> handleProjectionBannerToggled(event.enabled)
            is Event.OnAutoTrackingToggled -> handleAutoTrackingToggled(event.enabled)
            Event.OnManagePrivacyClicked -> launchEffect(Effect.ShowPrivacyOptions)
            Event.OnBackClicked -> launchEffect(Effect.NavigateBack)
            Event.OnDismissError -> updateState { copy(error = null) }
        }
    }

    private fun checkPremiumStatus() {
        isUserPremiumUseCase(IsUserPremiumUseCase.Input)
            .onEach { output ->
                if (output is IsUserPremiumUseCase.Output.Success) {
                    updateState { copy(isUserPremium = output.isPremium) }
                }
            }.launchIn(viewModelScope)
    }

    private fun loadPreferences() {
        getPreferencesUseCase(GetPreferencesUseCase.Input)
            .onEach { output ->
                if (output is GetPreferencesUseCase.Output.Success) {
                    updateState { 
                        copy(
                            rememberEmail = output.preferences.rememberEmail,
                            showProjectionBanner = output.preferences.showProjectionBanner,
                            autoTrackingEnabled = output.preferences.autoTrackingEnabled
                        ) 
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun handleRememberEmailToggled(enabled: Boolean) {
        updatePreferencesUseCase(UpdatePreferencesUseCase.Input(rememberEmail = enabled))
            .onEach { output ->
                if (output is UpdatePreferencesUseCase.Output.Success) {
                    updateState { copy(rememberEmail = enabled) }
                }
            }.launchIn(viewModelScope)
    }

    private fun handleProjectionBannerToggled(enabled: Boolean) {
        updatePreferencesUseCase(UpdatePreferencesUseCase.Input(showProjectionBanner = enabled))
            .onEach { output ->
                if (output is UpdatePreferencesUseCase.Output.Success) {
                    updateState { copy(showProjectionBanner = enabled) }
                }
            }.launchIn(viewModelScope)
    }

    private fun handleAutoTrackingToggled(enabled: Boolean) {
        updatePreferencesUseCase(UpdatePreferencesUseCase.Input(autoTrackingEnabled = enabled))
            .onEach { output ->
                if (output is UpdatePreferencesUseCase.Output.Success) {
                    updateState { copy(autoTrackingEnabled = enabled) }
                    if (enabled) {
                        autoTrackingManager.startAutoTracking()
                    } else {
                        autoTrackingManager.stopAutoTracking()
                    }
                }
            }.launchIn(viewModelScope)
    }
}
