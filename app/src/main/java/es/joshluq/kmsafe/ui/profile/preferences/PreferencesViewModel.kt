package es.joshluq.kmsafe.ui.profile.preferences

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.data.util.DeviceFingerprintProvider
import es.joshluq.kmsafe.di.*
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.usecase.*
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
    @param:GetEntitlements private val getEntitlementsUseCase:
    @JvmSuppressWildcards FlowUseCase<GetEntitlementsUseCase.Input, GetEntitlementsUseCase.Output>,
    @param:StartTrial private val startTrialUseCase:
    @JvmSuppressWildcards FlowUseCase<StartTrialUseCase.Input, StartTrialUseCase.Output>,
    private val fingerprintProvider: DeviceFingerprintProvider,
    @param:StartAutoTracking private val startAutoTrackingUseCase:
    @JvmSuppressWildcards FlowUseCase<StartAutoTrackingUseCase.Input, StartAutoTrackingUseCase.Output>,
    @param:StopAutoTracking private val stopAutoTrackingUseCase:
    @JvmSuppressWildcards FlowUseCase<StopAutoTrackingUseCase.Input, StopAutoTrackingUseCase.Output>,
    private val consentManager: ConsentManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    init {
        observeEntitlements()
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
            Event.OnPermissionsRationaleSuccess -> executeAutoTrackingToggle(true)
            Event.OnStartTrialClicked -> handleStartTrial()
            Event.OnDismissTrialOffer -> updateState { copy(showTrialOffer = false) }
        }
    }

    private fun observeEntitlements() {
        getEntitlementsUseCase(GetEntitlementsUseCase.Input("", forceRefresh = false))
            .onEach { output ->
                if (output is GetEntitlementsUseCase.Output.Success) {
                    val entitlements = output.entitlements
                    updateState {
                        copy(
                            isUserPremium = entitlements.isFeatureActive(Feature.AUTO_TRACKING),
                            canStartTrial = entitlements.isFeatureTrialable(Feature.AUTO_TRACKING)
                        )
                    }
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
        if (enabled && !state.value.isUserPremium && state.value.canStartTrial) {
            updateState { copy(showTrialOffer = true) }
            return
        }

        // If enabling, navigate to permissions rationale screen
        if (enabled) {
            launchEffect(Effect.NavigateToPermissions)
        } else {
            executeAutoTrackingToggle(false)
        }
    }

    private fun handleStartTrial() {
        updateState { copy(showTrialOffer = false, isLoading = true) }
        val fingerprint = fingerprintProvider.getFingerprint()
        startTrialUseCase(StartTrialUseCase.Input(fingerprint))
            .onEach { output ->
                when (output) {
                    is StartTrialUseCase.Output.Success -> {
                        updateState { copy(isLoading = false) }
                        // The observeEntitlements flow will update isUserPremium
                        // Now we can proceed to permissions
                        launchEffect(Effect.NavigateToPermissions)
                    }
                    is StartTrialUseCase.Output.Failure -> {
                        updateState {
                            copy(
                                isLoading = false,
                                error = TextProvider.Dynamic(output.message)
                            )
                        }
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun executeAutoTrackingToggle(enabled: Boolean) {
        updatePreferencesUseCase(UpdatePreferencesUseCase.Input(autoTrackingEnabled = enabled))
            .onEach { output ->
                if (output is UpdatePreferencesUseCase.Output.Success) {
                    updateState { copy(autoTrackingEnabled = enabled) }
                    if (enabled) {
                        startAutoTrackingUseCase(StartAutoTrackingUseCase.Input).launchIn(viewModelScope)
                    } else {
                        stopAutoTrackingUseCase(StopAutoTrackingUseCase.Input).launchIn(viewModelScope)
                    }
                }
            }.launchIn(viewModelScope)
    }
}
