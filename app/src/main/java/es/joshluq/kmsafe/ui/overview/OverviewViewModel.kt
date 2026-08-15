package es.joshluq.kmsafe.ui.overview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.di.AddOdometerRecord
import es.joshluq.kmsafe.di.ClearTracking
import es.joshluq.kmsafe.di.GetAllContracts
import es.joshluq.kmsafe.di.GetEntitlements
import es.joshluq.kmsafe.di.GetMonthlyUsage
import es.joshluq.kmsafe.di.GetOverviewData
import es.joshluq.kmsafe.di.GetPreferences
import es.joshluq.kmsafe.di.GetTripProjection
import es.joshluq.kmsafe.di.ObserveTrackingState
import es.joshluq.kmsafe.di.SelectContract
import es.joshluq.kmsafe.di.StartAutoTracking
import es.joshluq.kmsafe.di.StopAutoTracking
import es.joshluq.kmsafe.di.StopTracking
import es.joshluq.kmsafe.di.UpdatePreferences
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.usecase.AddOdometerRecordUseCase
import es.joshluq.kmsafe.domain.usecase.ClearTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.GetAllContractsUseCase
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.GetMonthlyUsageUseCase
import es.joshluq.kmsafe.domain.usecase.GetOverviewDataUseCase
import es.joshluq.kmsafe.domain.usecase.GetPreferencesUseCase
import es.joshluq.kmsafe.domain.usecase.GetTripProjectionUseCase
import es.joshluq.kmsafe.domain.usecase.ObserveTrackingStateUseCase
import es.joshluq.kmsafe.domain.usecase.SelectContractUseCase
import es.joshluq.kmsafe.domain.usecase.StartAutoTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StopAutoTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StopTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.UpdatePreferencesUseCase
import es.joshluq.kmsafe.ui.overview.model.toUiModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * ViewModel for the Overview screen, managing the business logic and state for vehicle metrics.
 */
@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    @param:GetOverviewData private val getOverviewDataUseCase:
    @JvmSuppressWildcards FlowUseCase<GetOverviewDataUseCase.Input, GetOverviewDataUseCase.Output>,
    @param:GetMonthlyUsage private val getMonthlyUsageUseCase:
    @JvmSuppressWildcards FlowUseCase<GetMonthlyUsageUseCase.Input, GetMonthlyUsageUseCase.Output>,
    @param:AddOdometerRecord private val addOdometerRecordUseCase:
    @JvmSuppressWildcards FlowUseCase<AddOdometerRecordUseCase.Input, AddOdometerRecordUseCase.Output>,
    @param:GetTripProjection private val getTripProjectionUseCase:
    @JvmSuppressWildcards FlowUseCase<GetTripProjectionUseCase.Input, GetTripProjectionUseCase.Output>,
    @param:GetAllContracts private val getAllContractsUseCase:
    @JvmSuppressWildcards FlowUseCase<GetAllContractsUseCase.Input, GetAllContractsUseCase.Output>,
    @param:SelectContract private val selectContractUseCase:
    @JvmSuppressWildcards FlowUseCase<SelectContractUseCase.Input, SelectContractUseCase.Output>,
    @param:GetEntitlements private val getEntitlementsUseCase:
    @JvmSuppressWildcards FlowUseCase<GetEntitlementsUseCase.Input, GetEntitlementsUseCase.Output>,
    @param:GetPreferences private val getPreferencesUseCase:
    @JvmSuppressWildcards FlowUseCase<GetPreferencesUseCase.Input, GetPreferencesUseCase.Output>,
    @param:UpdatePreferences private val updatePreferencesUseCase:
    @JvmSuppressWildcards FlowUseCase<UpdatePreferencesUseCase.Input, UpdatePreferencesUseCase.Output>,
    @param:ObserveTrackingState private val observeTrackingStateUseCase:
    @JvmSuppressWildcards FlowUseCase<ObserveTrackingStateUseCase.Input, ObserveTrackingStateUseCase.Output>,
    @param:StopTracking private val stopTrackingUseCase:
    @JvmSuppressWildcards FlowUseCase<StopTrackingUseCase.Input, StopTrackingUseCase.Output>,
    @param:ClearTracking private val clearTrackingUseCase:
    @JvmSuppressWildcards FlowUseCase<ClearTrackingUseCase.Input, ClearTrackingUseCase.Output>,
    @param:StartAutoTracking private val startAutoTrackingUseCase:
    @JvmSuppressWildcards FlowUseCase<StartAutoTrackingUseCase.Input, StartAutoTrackingUseCase.Output>,
    @param:StopAutoTracking private val stopAutoTrackingUseCase:
    @JvmSuppressWildcards FlowUseCase<StopAutoTrackingUseCase.Input, StopAutoTrackingUseCase.Output>,
    private val analytics: AnalyticskitManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    companion object {
        private const val DAYS_IN_MONTH = 30.4375
        private const val MILLIS_IN_DAY = 1000L * 60 * 60 * 24
    }

    private var bannerAlertJob: Job? = null

    init {
        syncInitialData()
        loadContractData()
        loadProjection()
        loadAllVehicles()
        observeTracking()
        observePermissionsResult()
    }

    private fun observePermissionsResult() {
        savedStateHandle.getStateFlow<Boolean?>("permissions_granted", null)
            .onEach { granted ->
                when (granted) {
                    true -> {
                        sendEvent(Event.OnPermissionsRationaleSuccess)
                        savedStateHandle.remove<Boolean>("permissions_granted")
                    }
                    false -> {
                        sendEvent(Event.OnAutoTrackingToggled(false))
                        savedStateHandle.remove<Boolean>("permissions_granted")
                    }
                    else -> {}
                }
            }.launchIn(viewModelScope)
    }

    override fun createInitialState(): State = State.Empty

    override fun handleEvent(event: Event) {
        logger.d("OverviewViewModel", "Event received: $event")
        when (event) {
            Event.OnRegisterRentingClicked -> handleOnRegisterRentingClicked()
            is Event.OnEditContractClicked -> {
                updateState { copy(showBluetoothSuggestionBanner = false) }
                launchEffect(Effect.NavigateToOnboarding(event.id, isEdit = true))
            }
            Event.OnUpdateOdometerClicked -> updateState {
                copy(
                    showBottomSheet = true,
                    newOdometerValue = "",
                    newRecordLabel = "",
                    newRecordFuel = "",
                    newRecordDate = System.currentTimeMillis()
                )
            }
            Event.OnBottomSheetDismissed -> updateState { copy(showBottomSheet = false) }
            Event.OnDismissProjectionBanner -> updateState { copy(showProjectionBanner = false) }
            Event.OnProjectionBannerClicked -> {
                analytics.track(AnalyticsEvent.Custom("projection_banner_clicked"))
                launchEffect(Effect.NavigateToProjection)
            }
            is Event.OnNewOdometerChanged -> updateState { copy(newOdometerValue = event.value) }
            is Event.OnNewLabelChanged -> updateState { copy(newRecordLabel = event.value) }
            is Event.OnNewFuelChanged -> updateState { copy(newRecordFuel = event.value) }
            is Event.OnSaveRecordClicked -> handleSaveRecord(event.timestamp)
            Event.OnToggleVehicleSwitcher -> updateState { copy(showVehicleSwitcher = !showVehicleSwitcher) }
            is Event.OnSwitchVehicleClicked -> handleOnSwitchVehicle(event.id)
            Event.OnDismissError -> updateState { copy(error = null) }
            Event.OnStartTrackingClicked -> handleStartTracking()
            Event.OnStopTrackingClicked -> handleStopTracking()
            Event.OnConfirmTrackedTripClicked -> handleConfirmTrackedTrip()
            Event.OnCancelTrackedTripClicked -> handleCancelTrackedTrip()
            Event.OnRequestPermissionsRationale -> launchEffect(Effect.NavigateToPermissions)
            Event.OnPermissionsRationaleSuccess -> handleAutoTrackingToggled(true)
            Event.OnPremiumUpgradeClicked -> {
                analytics.track(AnalyticsEvent.Custom("premium_upgrade_clicked", mapOf("source" to "top_bar")))
                launchEffect(Effect.NavigateToPremiumPaywall)
            }
            Event.OnDismissAutoTrackingPromotion -> {
                analytics.track(AnalyticsEvent.Custom("autotracking_promotion_dismissed"))
                handleDismissPromotion()
            }
            Event.OnAutoTrackingPromotionAccepted -> {
                analytics.track(AnalyticsEvent.Custom("autotracking_promotion_accepted"))
                handleDismissPromotion()
                launchEffect(Effect.NavigateToPreferences)
            }
            is Event.OnAutoTrackingToggled -> handleAutoTrackingToggled(event.enabled)
            Event.OnDismissBluetoothSuggestionBanner -> updateState { copy(showBluetoothSuggestionBanner = false) }
            Event.OnWelcomeGuideClicked -> launchEffect(Effect.NavigateToWelcomeDiscovery)
        }
    }

    private fun syncInitialData() {
        val entitlementsFlow = getEntitlementsUseCase(GetEntitlementsUseCase.Input("", forceRefresh = false))
            .distinctUntilChanged()
        val preferencesFlow = getPreferencesUseCase(GetPreferencesUseCase.Input)
            .distinctUntilChanged()

        combine(entitlementsFlow, preferencesFlow) { entitlementsOutput, preferencesOutput ->
            if (entitlementsOutput is GetEntitlementsUseCase.Output.Success &&
                preferencesOutput is GetPreferencesUseCase.Output.Success
            ) {
                val entitlements = entitlementsOutput.entitlements
                val prefs = preferencesOutput.preferences

                logger.i(
                    "OverviewViewModel",
                    "Initial data combined: SubLevel=${entitlements.subscriptionLevel}, AutoTracking=${prefs.autoTrackingEnabled}"
                )

                val hasPremiumAccess = entitlements.subscriptionLevel == SubscriptionLevel.PREMIUM
                val isTrialable = entitlements.isFeatureTrialable(Feature.AUTO_TRACKING)

                updateState {
                    copy(
                        isPremium = hasPremiumAccess,
                        isAutoTrackingTrialable = isTrialable,
                        subscriptionLevel = entitlements.subscriptionLevel,
                        autoTrackingEnabled = prefs.autoTrackingEnabled,
                        autoTrackingPromotionDismissed = prefs.autoTrackingPromotionDismissed
                    )
                }

                // Re-calculate metrics to refresh banner status with the new entitlements/prefs context
                state.value.renting?.let { calculateMetrics(it, (it.currentOdometer - it.startOdometer).toDouble()) }

                evaluatePromotion(
                    isPremium = hasPremiumAccess,
                    isTrialable = isTrialable,
                    isDismissed = prefs.autoTrackingPromotionDismissed
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun loadContractData() {
        getOverviewDataUseCase(GetOverviewDataUseCase.Input)
            .onEach { output ->
                when (output) {
                    is GetOverviewDataUseCase.Output.Success -> {
                        logger.i(
                            "OverviewViewModel",
                            "Active contract loaded: ${output.contract?.vehicleName ?: "No vehicle"}"
                        )
                        if (output.contract != null) {
                            calculateMetrics(output.contract, output.actualKmsDrivenSinceStart)
                            updateState { copy(isSyncPending = output.isSyncPending) }
                        } else {
                            updateState { copy(isLoading = false, renting = null) }
                        }
                    }
                    is GetOverviewDataUseCase.Output.Failure -> {
                        logger.e("OverviewViewModel", "Critical: Failed to load overview data")
                        updateState {
                            copy(
                                isLoading = false,
                                error = TextProvider.Resource(R.string.history_load_error)
                            )
                        }
                    }
                    is GetOverviewDataUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadAllVehicles() {
        getAllContractsUseCase(GetAllContractsUseCase.Input)
            .onEach { output ->
                when (output) {
                    is GetAllContractsUseCase.Output.Success -> updateState {
                        copy(
                            availableVehicles = output.contracts
                        )
                    }
                    else -> { }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleOnSwitchVehicle(id: String) {
        selectContractUseCase(SelectContractUseCase.Input(id))
            .onEach { output ->
                when (output) {
                    is SelectContractUseCase.Output.Success -> updateState { copy(showVehicleSwitcher = false) }
                    is SelectContractUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                    is SelectContractUseCase.Output.Failure -> updateState { copy(isLoading = false) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadMonthlyUsage() {
        getMonthlyUsageUseCase(GetMonthlyUsageUseCase.Input)
            .onEach { output ->
                when (output) {
                    is GetMonthlyUsageUseCase.Output.Success -> {
                        val uiModels = output.aggregations.map { it.toUiModel() }
                        updateState { copy(monthlyUsage = uiModels, isLoading = false) }
                    }
                    is GetMonthlyUsageUseCase.Output.Failure -> updateState { copy(isLoading = false) }
                    else -> { }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadProjection() {
        getTripProjectionUseCase(GetTripProjectionUseCase.Input)
            .onEach { output ->
                if (output is GetTripProjectionUseCase.Output.Success) {
                    val currentOverLimit = output.projection?.isOverLimit ?: false
                    updateState { copy(projection = output.projection) }
                    checkBannerAlert(currentOverLimit)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun checkBannerAlert(currentOverLimit: Boolean) {
        bannerAlertJob?.cancel()
        bannerAlertJob = getPreferencesUseCase(GetPreferencesUseCase.Input)
            .onEach { output ->
                if (output is GetPreferencesUseCase.Output.Success) {
                    val prefs = output.preferences
                    if (prefs.showProjectionBanner) {
                        val lastState = prefs.lastKnownOverLimit
                        if (lastState == null || lastState != currentOverLimit) {
                            updateState { copy(showProjectionBanner = true) }
                            updatePreferencesUseCase(
                                UpdatePreferencesUseCase.Input(lastKnownOverLimit = currentOverLimit)
                            )
                                .launchIn(viewModelScope)
                        }
                    } else {
                        updateState { copy(showProjectionBanner = false) }
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun evaluatePromotion(isPremium: Boolean, isTrialable: Boolean, isDismissed: Boolean) {
        if (isDismissed) {
            logger.i("OverviewViewModel", "Auto-tracking promotion evaluation: Skipping")
            updateState { copy(showAutoTrackingPromotion = false) }
            return
        }

        val shouldShow = isPremium || isTrialable
        logger.i("OverviewViewModel", "Auto-tracking promotion evaluation: $shouldShow")
        updateState { copy(showAutoTrackingPromotion = shouldShow) }
    }

    private fun handleDismissPromotion() {
        updateState { copy(showAutoTrackingPromotion = false) }
        updatePreferencesUseCase(UpdatePreferencesUseCase.Input(autoTrackingPromotionDismissed = true))
            .launchIn(viewModelScope)
    }

    private fun handleSaveRecord(timestamp: Long) {
        val odometerValue = state.value.newOdometerValue.toIntOrNull() ?: return
        val label = state.value.newRecordLabel.takeIf { it.isNotBlank() }
        val fuelAmount = state.value.newRecordFuel.toDoubleOrNull()

        logger.i("OverviewViewModel", "Initiating odometer save: $odometerValue km, Label=$label")

        addOdometerRecordUseCase(
            AddOdometerRecordUseCase.Input(
                odometerValue = odometerValue,
                timestamp = timestamp,
                label = label,
                fuelAmount = fuelAmount,
                encodedPolyline = state.value.currentRoutePolyline,
                pointCount = state.value.currentPointCount
            )
        )
            .onEach { output ->
                when (output) {
                    is AddOdometerRecordUseCase.Output.Progress -> updateState { copy(isSaving = true) }
                    is AddOdometerRecordUseCase.Output.Success -> {
                        logger.i("OverviewViewModel", "Odometer record saved successfully")
                        analytics.track(AnalyticsEvent.Custom("odometer_updated", mapOf("value" to odometerValue)))
                        if (fuelAmount != null) {
                            analytics.track(AnalyticsEvent.Custom("fuel_entry_added", mapOf("amount" to fuelAmount)))
                        }
                        updateState { copy(isSaving = false, showBottomSheet = false, isLoading = false) }
                        clearTrackingUseCase(ClearTrackingUseCase.Input).launchIn(viewModelScope)
                    }
                    is AddOdometerRecordUseCase.Output.Failure -> {
                        updateState {
                            copy(
                                isSaving = false,
                                error = TextProvider.Dynamic(output.message)
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun calculateMetrics(renting: RentingContract, actualKmsDrivenSinceStart: Double) {
        val currentTime = System.currentTimeMillis()
        val totalDays = renting.durationMonths * DAYS_IN_MONTH
        val daysPassed = (currentTime - renting.startDate) / MILLIS_IN_DAY.toDouble()
            .coerceAtLeast(0.0)
        val baseDailyBudget = renting.totalKms / totalDays
        val monthlyBudget = renting.totalKms.toDouble() / renting.durationMonths
        val theoreticalKms = daysPassed * baseDailyBudget
        val balance = theoreticalKms - actualKmsDrivenSinceStart

        logger.d(
            "OverviewViewModel",
            "Metrics re-calculated for ${renting.vehicleName}: Balance=${balance.toInt()}, DaysPassed=${daysPassed.toInt()}"
        )

        val timeUsedPercentage = (daysPassed / totalDays).coerceIn(0.0, 1.0).toFloat()
        val kmsUsedPercentage = (actualKmsDrivenSinceStart / renting.totalKms).coerceIn(0.0, 1.0).toFloat()
        val differencePercentage = ((timeUsedPercentage - kmsUsedPercentage) * 100)
        val currentOdometer = renting.startOdometer + actualKmsDrivenSinceStart

        // Bluetooth Suggestion Evaluation (Single Source of Truth)
        val isPremium = state.value.isPremium ?: false
        val isAutoTrackingEnabled = state.value.autoTrackingEnabled
        val hasBluetooth = renting.bluetoothDeviceAddress != null
        val showBluetoothSuggestion = isPremium && isAutoTrackingEnabled && !hasBluetooth

        logger.d(
            "OverviewViewModel",
            "Banner Eval: isPremium=$isPremium, autoTrack=$isAutoTrackingEnabled, hasBT=$hasBluetooth -> showBanner=$showBluetoothSuggestion"
        )

        updateState {
            copy(
                renting = renting,
                balance = balance.toInt(),
                dailyLimit = baseDailyBudget.toInt(),
                monthlyLimit = monthlyBudget.toInt(),
                totalKmsDriven = currentOdometer.toInt(),
                timePercentage = timeUsedPercentage,
                kmsPercentage = kmsUsedPercentage,
                differencePercentage = differencePercentage,
                showBluetoothSuggestionBanner = showBluetoothSuggestion
            )
        }
        loadMonthlyUsage()
    }

    private fun handleOnRegisterRentingClicked() {
        launchEffect(Effect.NavigateToOnboarding())
    }

    private fun observeTracking() {
        observeTrackingStateUseCase(ObserveTrackingStateUseCase.Input)
            .onEach { output ->
                if (output is ObserveTrackingStateUseCase.Output.Success) {
                    updateState {
                        copy(
                            isTracking = output.isTracking,
                            trackedDistance = output.trackedDistance,
                            tripStartTime = output.startTime,
                            currentRoutePolyline = output.encodedPolyline,
                            currentPointCount = output.pointCount
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleStartTracking() {
        analytics.track(AnalyticsEvent.Custom("tracking_started"))
        launchEffect(Effect.StartTrackingService)
    }

    private fun handleStopTracking() {
        logger.d("OverviewViewModel", "handleStopTracking called. Launching Effect.StopTrackingService")
        analytics.track(AnalyticsEvent.Custom("tracking_stopped", mapOf("distance" to state.value.trackedDistance)))
        launchEffect(Effect.StopTrackingService)
    }

    private fun handleConfirmTrackedTrip() {
        val totalKms = state.value.trackedDistance / 1000.0
        val tripKms = totalKms.toInt()
        updateState {
            copy(
                showBottomSheet = true,
                newOdometerValue = tripKms.toString()
            )
        }
        stopTrackingUseCase(StopTrackingUseCase.Input).launchIn(viewModelScope)
    }

    private fun handleCancelTrackedTrip() {
        analytics.track(AnalyticsEvent.Custom("tracking_cancelled"))
        clearTrackingUseCase(ClearTrackingUseCase.Input).launchIn(viewModelScope)
    }

    private fun handleAutoTrackingToggled(enabled: Boolean) {
        logger.i("OverviewViewModel", "Auto-tracking preference toggled to: $enabled")
        updatePreferencesUseCase(UpdatePreferencesUseCase.Input(autoTrackingEnabled = enabled))
            .onEach { output ->
                if (output is UpdatePreferencesUseCase.Output.Success) {
                    updateState { copy(autoTrackingEnabled = enabled) }

                    if (enabled) {
                        logger.d("OverviewViewModel", "Starting auto-tracking sensors")
                        startAutoTrackingUseCase(StartAutoTrackingUseCase.Input).launchIn(viewModelScope)
                    } else {
                        logger.d("OverviewViewModel", "Stopping auto-tracking sensors")
                        stopAutoTrackingUseCase(StopAutoTrackingUseCase.Input).launchIn(viewModelScope)
                    }

                    // Re-trigger evaluation of the banner after toggle
                    state.value.renting?.let {
                        calculateMetrics(
                            it,
                            (it.currentOdometer - it.startOdometer).toDouble()
                        )
                    }
                }
            }.launchIn(viewModelScope)
    }
}
