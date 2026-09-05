package es.joshluq.kmsafe.feature.overview

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.core.domain.usecase.SyncStationGeofencesUseCase
import es.joshluq.kmsafe.core.monetization.domain.MonetizationConfig
import es.joshluq.kmsafe.core.ui.util.DateUtils.getContractEndDate
import es.joshluq.kmsafe.core.ui.util.DateUtils.normalizeToUtc00
import es.joshluq.kmsafe.domain.model.Feature
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
import es.joshluq.kmsafe.domain.usecase.ObserveVehicleBluetoothConnectionUseCase
import es.joshluq.kmsafe.domain.usecase.SelectContractUseCase
import es.joshluq.kmsafe.domain.usecase.StartAutoTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StartTripTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StopAutoTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StopTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StopTripTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.UpdatePreferencesUseCase
import es.joshluq.kmsafe.feature.overview.model.toUiModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Overview screen, managing the business logic and state for vehicle metrics.
 */
@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val getOverviewDataUseCase: GetOverviewDataUseCase,
    private val getMonthlyUsageUseCase: GetMonthlyUsageUseCase,
    private val addOdometerRecordUseCase: AddOdometerRecordUseCase,
    private val getTripProjectionUseCase: GetTripProjectionUseCase,
    private val getAllContractsUseCase: GetAllContractsUseCase,
    private val selectContractUseCase: SelectContractUseCase,
    private val getEntitlementsUseCase: GetEntitlementsUseCase,
    private val getPreferencesUseCase: GetPreferencesUseCase,
    private val updatePreferencesUseCase: UpdatePreferencesUseCase,
    private val observeTrackingStateUseCase: ObserveTrackingStateUseCase,
    private val observeVehicleBluetoothConnectionUseCase: ObserveVehicleBluetoothConnectionUseCase,
    private val stopTrackingUseCase: StopTrackingUseCase,
    private val clearTrackingUseCase: ClearTrackingUseCase,
    private val startAutoTrackingUseCase: StartAutoTrackingUseCase,
    private val stopAutoTrackingUseCase: StopAutoTrackingUseCase,
    private val startTripTrackingUseCase: StartTripTrackingUseCase,
    private val stopTripTrackingUseCase: StopTripTrackingUseCase,
    private val syncStationGeofencesUseCase: SyncStationGeofencesUseCase,
    private val monetizationConfig: MonetizationConfig,
    private val analytics: AnalyticskitManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    private var bannerAlertJob: Job? = null
    private var bluetoothJob: Job? = null

    init {
        consolidatedInitialLoad()
        observeTracking()
        startGeofenceSync()
        updateState { copy(adUnitId = monetizationConfig.getBannerAdUnitId()) }
    }

    private fun startGeofenceSync() {
        syncStationGeofencesUseCase(SyncStationGeofencesUseCase.Input)
            .launchIn(viewModelScope)
    }

    private fun consolidatedInitialLoad() {
        val entitlementsFlow = getEntitlementsUseCase(GetEntitlementsUseCase.Input("", forceRefresh = false))
            .distinctUntilChanged()
        val preferencesFlow = getPreferencesUseCase(GetPreferencesUseCase.Input)
            .distinctUntilChanged()
        val overviewDataFlow = getOverviewDataUseCase(GetOverviewDataUseCase.Input)
            .distinctUntilChanged()
        val allContractsFlow = getAllContractsUseCase(GetAllContractsUseCase.Input)
            .distinctUntilChanged()

        combine(
            entitlementsFlow,
            preferencesFlow,
            overviewDataFlow,
            allContractsFlow
        ) { entitlementsOutput, preferencesOutput, overviewOutput, allContractsOutput ->

            // 1. Process Entitlements & Preferences
            var newState = state.value

            if (entitlementsOutput is GetEntitlementsUseCase.Output.Success &&
                preferencesOutput is GetPreferencesUseCase.Output.Success
            ) {
                val entitlements = entitlementsOutput.entitlements
                val prefs = preferencesOutput.preferences
                val isPremiumUser = entitlements.subscriptionLevel == SubscriptionLevel.PREMIUM
                val isTrialable = entitlements.isFeatureTrialable(Feature.AUTO_TRACKING)
                val isAutoTrackEnabled = prefs.autoTrackingEnabled
                val isPromoDismissed = prefs.autoTrackingPromotionDismissed

                val shouldShowPromo = (isPremiumUser || isTrialable) && !isAutoTrackEnabled && !isPromoDismissed

                if (isAutoTrackEnabled) {
                    logger.d("OverviewViewModel", "Auto-tracking is enabled in preferences. Ensuring registration.")
                    startAutoTrackingUseCase(StartAutoTrackingUseCase.Input).launchIn(viewModelScope)
                }

                newState = newState.copy(
                    isPremium = isPremiumUser,
                    isAutoTrackingTrialable = isTrialable,
                    subscriptionLevel = entitlements.subscriptionLevel,
                    autoTrackingEnabled = isAutoTrackEnabled,
                    autoTrackingPromotionDismissed = isPromoDismissed,
                    showAutoTrackingPromotion = shouldShowPromo
                )
            }

            // 2. Process Contract Data
            if (overviewOutput is GetOverviewDataUseCase.Output.Success) {
                val contract = overviewOutput.contract
                val metrics = overviewOutput.metrics
                if (contract != null && metrics != null) {
                    val isPremium = newState.isPremium ?: false
                    val isAutoTrackingEnabled = newState.autoTrackingEnabled
                    val hasBluetooth = contract.bluetoothDeviceAddress != null
                    val showBluetoothSuggestion = isPremium && isAutoTrackingEnabled && !hasBluetooth

                    newState = newState.copy(
                        renting = contract,
                        balance = metrics.balance,
                        dailyLimit = metrics.dailyBudget,
                        monthlyLimit = metrics.monthlyBudget,
                        totalKmsDriven = metrics.currentOdometer,
                        actualKmsDriven = metrics.actualKmsDriven,
                        timePercentage = metrics.timePercentage,
                        kmsPercentage = metrics.kmsPercentage,
                        differencePercentage = metrics.differencePercentage,
                        showBluetoothSuggestionBanner = showBluetoothSuggestion,
                        isSyncPending = metrics.isSyncPending,
                        isLoading = false
                    )
                } else {
                    newState = newState.copy(isLoading = false, renting = null)
                }
            } else if (overviewOutput is GetOverviewDataUseCase.Output.Progress) {
                // To avoid flickering, we only show loading if it takes too long
                // or if we explicitly want to force a loader when there's no data.
                // For now, we only set it if we don't have a vehicle.
                if (newState.renting == null && state.value.renting == null) {
                    newState = newState.copy(isLoading = true)
                }
            }

            // 3. Process Available Vehicles
            if (allContractsOutput is GetAllContractsUseCase.Output.Success) {
                newState = newState.copy(availableVehicles = allContractsOutput.contracts)
            }

            // Atomically update the state once per combine emission
            val oldRentingId = state.value.renting?.id
            val newRentingId = newState.renting?.id
            val oldMac = state.value.renting?.bluetoothDeviceAddress
            val newMac = newState.renting?.bluetoothDeviceAddress

            if (newState != state.value) {
                updateState { newState }

                if (newRentingId != null && newRentingId != oldRentingId) {
                    loadMonthlyUsage()
                    loadProjection()
                }
            }

            if (newMac != oldMac || bluetoothJob == null) {
                observeBluetoothForVehicle(newMac)
            }
        }.launchIn(viewModelScope)
    }

    private fun observeBluetoothForVehicle(macAddress: String?) {
        bluetoothJob?.cancel()
        bluetoothJob = observeVehicleBluetoothConnectionUseCase(
            ObserveVehicleBluetoothConnectionUseCase.Input(macAddress)
        ).onEach { output ->
            if (output is ObserveVehicleBluetoothConnectionUseCase.Output.Success) {
                updateState { copy(isVehicleBluetoothConnected = output.isConnected) }
            }
        }.launchIn(viewModelScope)
    }

    override fun createInitialState(): State = State.Empty

    override fun handleEvent(event: Event) {
        logger.d("OverviewViewModel", "Event received: $event")
        when (event) {
            Event.OnRegisterRentingClicked -> handleOnRegisterRentingClicked()
            is Event.OnVehicleDetailClicked -> {
                launchEffect(Effect.NavigateToVehicleDetail(event.id))
            }
            is Event.OnEditContractClicked -> {
                updateState { copy(showBluetoothSuggestionBanner = false) }
                launchEffect(Effect.NavigateToOnboarding(event.id, isEdit = true))
            }
            Event.OnUpdateOdometerClicked -> {
                val initialDate = normalizeToUtc00(System.currentTimeMillis())
                val isValid = validateRecordDate(initialDate)
                updateState {
                    copy(
                        showBottomSheet = true,
                        newOdometerValue = "",
                        newRecordLabel = "",
                        newRecordFuel = "",
                        newRecordDate = initialDate,
                        newRecordDateError = if (isValid) {
                            null
                        } else {
                            TextProvider.Resource(
                                R.string.overview_error_date_outside_contract
                            )
                        },
                        currentRoutePolyline = null,
                        currentPointCount = 0
                    )
                }
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
            is Event.OnNewRecordDateChanged -> handleNewRecordDateChanged(event.timestamp)
            is Event.OnSaveRecordClicked -> handleSaveRecord(event.timestamp)
            Event.OnToggleVehicleSwitcher -> updateState { copy(showVehicleSwitcher = !showVehicleSwitcher) }
            is Event.OnSwitchVehicleClicked -> handleOnSwitchVehicle(event.id)
            Event.OnDismissError -> updateState { copy(error = null) }
            Event.OnStartTrackingClicked -> handleStartTracking()
            Event.OnStopTrackingClicked -> handleStopTracking()
            Event.OnConfirmTrackedTripClicked -> handleConfirmTrackedTrip()
            Event.OnCancelTrackedTripClicked -> handleCancelTrackedTrip()
            Event.OnRequestPermissionsRationale -> launchEffect(Effect.NavigateToPermissions)
            is Event.OnPermissionsResult -> handlePermissionsResult(event.granted)
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

    private fun handlePermissionsResult(granted: Boolean) {
        if (granted) {
            handleAutoTrackingToggled(true)
        } else {
            handleAutoTrackingToggled(false)
        }
    }

    private fun handleOnSwitchVehicle(id: String) {
        selectContractUseCase(SelectContractUseCase.Input(id))
            .onEach { output ->
                when (output) {
                    is SelectContractUseCase.Output.Success -> updateState {
                        copy(showVehicleSwitcher = false)
                    }
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

    private fun handleDismissPromotion() {
        updateState { copy(showAutoTrackingPromotion = false) }
        updatePreferencesUseCase(UpdatePreferencesUseCase.Input(autoTrackingPromotionDismissed = true))
            .launchIn(viewModelScope)
    }

    private fun handleSaveRecord(timestamp: Long) {
        if (!validateRecordDate(timestamp)) {
            updateState {
                copy(newRecordDateError = TextProvider.Resource(R.string.overview_error_date_outside_contract))
            }
            return
        }

        val odometerValue = state.value.newOdometerValue.replace(',', '.').toDoubleOrNull() ?: return
        val label = state.value.newRecordLabel.takeIf { it.isNotBlank() }
        val fuelAmount = state.value.newRecordFuel.replace(',', '.').toDoubleOrNull()

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

    private fun handleNewRecordDateChanged(timestamp: Long) {
        val isValid = validateRecordDate(timestamp)
        updateState {
            copy(
                newRecordDate = timestamp,
                newRecordDateError = if (isValid) {
                    null
                } else {
                    TextProvider.Resource(
                        R.string.overview_error_date_outside_contract
                    )
                }
            )
        }
    }

    private fun validateRecordDate(timestamp: Long): Boolean {
        val contract = state.value.renting ?: return true
        val startDate = normalizeToUtc00(contract.startDate)
        val endDate = normalizeToUtc00(getContractEndDate(startDate, contract.durationMonths))
        val normalizedTimestamp = normalizeToUtc00(timestamp)

        return normalizedTimestamp in startDate..endDate
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
        viewModelScope.launch {
            startTripTrackingUseCase(StartTripTrackingUseCase.Input)
        }
    }

    private fun handleStopTracking() {
        logger.d("OverviewViewModel", "handleStopTracking called")
        analytics.track(AnalyticsEvent.Custom("tracking_stopped", mapOf("distance" to state.value.trackedDistance)))
        viewModelScope.launch {
            stopTripTrackingUseCase(StopTripTrackingUseCase.Input)
        }
    }

    private fun handleConfirmTrackedTrip() {
        val tripKms = state.value.trackedDistance / 1000.0
        updateState {
            copy(
                showBottomSheet = true,
                newOdometerValue = String.format(java.util.Locale.getDefault(), "%.2f", tripKms)
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
                    val isPremium = state.value.isPremium ?: false
                    val hasBluetooth = state.value.renting?.bluetoothDeviceAddress != null
                    val showBluetooth = isPremium && enabled && !hasBluetooth
                    updateState { copy(showBluetoothSuggestionBanner = showBluetooth) }
                }
            }.launchIn(viewModelScope)
    }
}
