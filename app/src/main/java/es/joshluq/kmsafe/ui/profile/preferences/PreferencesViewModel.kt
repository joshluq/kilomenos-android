package es.joshluq.kmsafe.ui.profile.preferences

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.core.monetization.util.ConsentManager
import es.joshluq.kmsafe.domain.di.GetEntitlements
import es.joshluq.kmsafe.domain.di.GetPreferences
import es.joshluq.kmsafe.domain.di.StartAutoTracking
import es.joshluq.kmsafe.domain.di.StartTrial
import es.joshluq.kmsafe.domain.di.StopAutoTracking
import es.joshluq.kmsafe.domain.di.UpdatePreferences
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.service.FingerprintProvider
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.GetPreferencesUseCase
import es.joshluq.kmsafe.domain.usecase.StartAutoTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StartTrialUseCase
import es.joshluq.kmsafe.domain.usecase.StopAutoTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.UpdatePreferencesUseCase
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
    private val fingerprintProvider: FingerprintProvider,
    @param:StartAutoTracking private val startAutoTrackingUseCase:
    @JvmSuppressWildcards FlowUseCase<StartAutoTrackingUseCase.Input, StartAutoTrackingUseCase.Output>,
    @param:StopAutoTracking private val stopAutoTrackingUseCase:
    @JvmSuppressWildcards FlowUseCase<StopAutoTrackingUseCase.Input, StopAutoTrackingUseCase.Output>,
    private val consentManager: ConsentManager,
    private val analytics: AnalyticskitManager,
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
            is Event.OnRememberEmailToggled -> {
                analytics.track(AnalyticsEvent.Custom("remember_email_toggled", mapOf("enabled" to event.enabled)))
                handleRememberEmailToggled(event.enabled)
            }
            is Event.OnProjectionBannerToggled -> {
                analytics.track(AnalyticsEvent.Custom("projection_banner_toggled", mapOf("enabled" to event.enabled)))
                handleProjectionBannerToggled(event.enabled)
            }
            is Event.OnAutoTrackingToggled -> {
                logger.i("PreferencesViewModel", "Auto-tracking toggle requested: ${event.enabled}")
                analytics.track(AnalyticsEvent.Custom("autotracking_toggled_intent", mapOf("enabled" to event.enabled)))
                handleAutoTrackingToggled(event.enabled)
            }
            is Event.OnPermissionsResult -> handlePermissionsResult(event.granted)
            Event.OnManagePrivacyClicked -> {
                analytics.track(AnalyticsEvent.Custom("manage_privacy_clicked"))
                launchEffect(Effect.ShowPrivacyOptions)
            }
            Event.OnBackClicked -> launchEffect(Effect.NavigateBack)
            Event.OnDismissError -> updateState { copy(error = null) }
            Event.OnStartTrialClicked -> handleStartTrial()
            Event.OnDismissTrialOffer -> {
                analytics.track(AnalyticsEvent.Custom("premium_trial_offer_dismissed"))
                updateState { copy(showTrialOffer = false) }
            }
        }
    }

    private fun handlePermissionsResult(granted: Boolean) {
        logger.i("PreferencesViewModel", "Permissions result received: $granted")
        if (granted) {
            executeAutoTrackingToggle(true)
        } else {
            logger.w("PreferencesViewModel", "Permissions denied. Ensuring toggle OFF.")
            updateState { copy(autoTrackingEnabled = false) }
        }
    }

    private fun observeEntitlements() {
        getEntitlementsUseCase(GetEntitlementsUseCase.Input("", forceRefresh = false))
            .onEach { output ->
                if (output is GetEntitlementsUseCase.Output.Success) {
                    val entitlements = output.entitlements
                    logger.d("PreferencesViewModel", "Entitlements updated: $entitlements")
                    updateState {
                        copy(
                            isUserPremium = entitlements.isFeatureActive(Feature.AUTO_TRACKING),
                            canStartTrial = entitlements.isFeatureTrialable(Feature.AUTO_TRACKING),
                            isEntitlementsLoaded = true
                        )
                    }
                    checkAndHealAutoTracking()
                }
            }.launchIn(viewModelScope)
    }

    private fun loadPreferences() {
        getPreferencesUseCase(GetPreferencesUseCase.Input)
            .onEach { output ->
                if (output is GetPreferencesUseCase.Output.Success) {
                    logger.d("PreferencesViewModel", "Preferences loaded from source: autoTracking=${output.preferences.autoTrackingEnabled}")
                    updateState {
                        copy(
                            rememberEmail = output.preferences.rememberEmail,
                            showProjectionBanner = output.preferences.showProjectionBanner,
                            autoTrackingEnabled = output.preferences.autoTrackingEnabled
                        )
                    }
                    checkAndHealAutoTracking()
                }
            }.launchIn(viewModelScope)
    }

    private fun checkAndHealAutoTracking() {
        val currentState = state.value
        if (currentState.isEntitlementsLoaded && !currentState.isUserPremium && currentState.autoTrackingEnabled) {
            logger.w("PreferencesViewModel", "Self-healing triggered: User is FREE but auto-tracking was ON. Disabling.")
            executeAutoTrackingToggle(false)
        }
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
            logger.i("PreferencesViewModel", "User is FREE but feature is trialable. Showing offer.")
            updateState { copy(showTrialOffer = true) }
            return
        }

        // If enabling, navigate to permissions rationale screen
        if (enabled) {
            logger.i("PreferencesViewModel", "Navigating to permissions screen")
            // Optimistic update so the switch stays ON while navigating
            updateState { copy(autoTrackingEnabled = true) }
            launchEffect(Effect.NavigateToPermissions)
        } else {
            logger.i("PreferencesViewModel", "Disabling auto-tracking directly")
            executeAutoTrackingToggle(false)
        }
    }

    private fun handleStartTrial() {
        analytics.track(AnalyticsEvent.Custom("premium_trial_started"))
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
        logger.d("PreferencesViewModel", "Executing preference update: autoTracking=$enabled")
        // Optimistic update to UI state
        updateState { copy(autoTrackingEnabled = enabled) }

        updatePreferencesUseCase(UpdatePreferencesUseCase.Input(autoTrackingEnabled = enabled))
            .onEach { output ->
                if (output is UpdatePreferencesUseCase.Output.Success) {
                    logger.i("PreferencesViewModel", "Preference update SUCCESS: autoTracking=$enabled")
                    if (enabled) {
                        startAutoTrackingUseCase(StartAutoTrackingUseCase.Input).launchIn(viewModelScope)
                    } else {
                        stopAutoTrackingUseCase(StopAutoTrackingUseCase.Input).launchIn(viewModelScope)
                    }
                }
            }.launchIn(viewModelScope)
    }
}
