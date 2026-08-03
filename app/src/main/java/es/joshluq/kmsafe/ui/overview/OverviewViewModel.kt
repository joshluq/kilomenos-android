package es.joshluq.kmsafe.ui.overview

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
import es.joshluq.kmsafe.di.GetAllContracts
import es.joshluq.kmsafe.di.GetMonthlyUsage
import es.joshluq.kmsafe.di.GetOverviewData
import es.joshluq.kmsafe.di.GetPreferences
import es.joshluq.kmsafe.di.GetTripProjection
import es.joshluq.kmsafe.di.IsUserPremium
import es.joshluq.kmsafe.di.SelectContract
import es.joshluq.kmsafe.di.UpdatePreferences
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import es.joshluq.kmsafe.domain.usecase.AddOdometerRecordUseCase
import es.joshluq.kmsafe.domain.usecase.GetAllContractsUseCase
import es.joshluq.kmsafe.domain.usecase.GetMonthlyUsageUseCase
import es.joshluq.kmsafe.domain.usecase.GetOverviewDataUseCase
import es.joshluq.kmsafe.domain.usecase.GetPreferencesUseCase
import es.joshluq.kmsafe.domain.usecase.GetTripProjectionUseCase
import es.joshluq.kmsafe.domain.usecase.IsUserPremiumUseCase
import es.joshluq.kmsafe.domain.usecase.SelectContractUseCase
import es.joshluq.kmsafe.domain.usecase.UpdatePreferencesUseCase
import es.joshluq.kmsafe.ui.overview.model.toUiModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * ViewModel for the Overview screen, managing the business logic and state for vehicle metrics.
 */
@HiltViewModel
class OverviewViewModel @Inject constructor(
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
    @param:IsUserPremium private val isUserPremiumUseCase:
    @JvmSuppressWildcards FlowUseCase<IsUserPremiumUseCase.Input, IsUserPremiumUseCase.Output>,
    @param:GetPreferences private val getPreferencesUseCase:
    @JvmSuppressWildcards FlowUseCase<GetPreferencesUseCase.Input, GetPreferencesUseCase.Output>,
    @param:UpdatePreferences private val updatePreferencesUseCase:
    @JvmSuppressWildcards FlowUseCase<UpdatePreferencesUseCase.Input, UpdatePreferencesUseCase.Output>,
    private val trackingRepository: TrackingRepository,
    private val analytics: AnalyticskitManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    companion object {
        private const val DAYS_IN_MONTH = 30.4375
        private const val MILLIS_IN_DAY = 1000L * 60 * 60 * 24
    }

    init {
        checkSubscription()
        loadContractData()
        loadProjection()
        loadAllVehicles()
        observeTracking()
    }

    override fun createInitialState(): State = State.Empty

    override fun handleEvent(event: Event) {
        logger.d("OverviewViewModel", "Event received: $event")
        when (event) {
            Event.OnRegisterRentingClicked -> handleOnRegisterRentingClicked()
            is Event.OnEditContractClicked -> launchEffect(Effect.NavigateToOnboarding(event.id, isEdit = true))
            Event.OnUpdateOdometerClicked -> updateState {
                copy(
                    showBottomSheet = true,
                    newOdometerValue = "",
                    newRecordLabel = "",
                    newRecordFuel = ""
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
            Event.OnCancelTrackedTripClicked -> {
                analytics.track(AnalyticsEvent.Custom("tracking_cancelled"))
                trackingRepository.clear()
            }
        }
    }

    private fun checkSubscription() {
        isUserPremiumUseCase(IsUserPremiumUseCase.Input)
            .onEach { output ->
                if (output is IsUserPremiumUseCase.Output.Success) {
                    val isPremium = output.isPremium
                    updateState { copy(isPremium = isPremium) }
                    // Refresh data to ensure sync icon is updated based on new plan
                    loadContractData()
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Loads the contract and odometer data to calculate the current status.
     */
    private fun loadContractData() {
        getOverviewDataUseCase(GetOverviewDataUseCase.Input)
            .onEach { output ->
                when (output) {
                    is GetOverviewDataUseCase.Output.Success -> {
                        if (output.contract != null) {
                            calculateMetrics(output.contract, output.actualKmsDrivenSinceStart)
                            // Indicator should only be visible for Premium users
                            updateState { copy(isSyncPending = isPremium && output.isSyncPending) }
                        } else {
                            // Important: Don't use State.Empty here as it wipes availableVehicles
                            updateState { copy(isLoading = false, renting = null) }
                        }
                    }
                    is GetOverviewDataUseCase.Output.Failure -> updateState { 
                        copy(
                            isLoading = false,
                            error = TextProvider.Resource(R.string.history_load_error)
                        ) 
                    }
                    is GetOverviewDataUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Loads all registered vehicles to allow switching.
     */
    private fun loadAllVehicles() {
        getAllContractsUseCase(GetAllContractsUseCase.Input)
            .onEach { output ->
                when (output) {
                    is GetAllContractsUseCase.Output.Success -> updateState {
                        copy(
                            availableVehicles = output.contracts
                        )
                    }
                    else -> { /* Ignore errors for this secondary view */ }
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

    /**
     * Loads the predictive projection.
     */
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
        getPreferencesUseCase(GetPreferencesUseCase.Input)
            .onEach { output ->
                if (output is GetPreferencesUseCase.Output.Success) {
                    val prefs = output.preferences
                    if (prefs.showProjectionBanner) {
                        val lastState = prefs.lastKnownOverLimit
                        if (lastState == null || lastState != currentOverLimit) {
                            // Significant change or first time -> Show the banner
                            updateState { copy(showProjectionBanner = true) }
                            // Persist the new state as last known
                            updatePreferencesUseCase(UpdatePreferencesUseCase.Input(lastKnownOverLimit = currentOverLimit))
                                .launchIn(viewModelScope)
                        }
                    } else {
                        updateState { copy(showProjectionBanner = false) }
                    }
                }
            }.launchIn(viewModelScope)
    }

    /**
     * Persists a new odometer record.
     */
    private fun handleSaveRecord(timestamp: Long) {
        val odometerValue = state.value.newOdometerValue.toIntOrNull() ?: return
        val label = state.value.newRecordLabel.takeIf { it.isNotBlank() }
        val fuelAmount = state.value.newRecordFuel.toDoubleOrNull()

        addOdometerRecordUseCase(
            AddOdometerRecordUseCase.Input(
                odometerValue = odometerValue,
                timestamp = timestamp,
                label = label,
                fuelAmount = fuelAmount
            )
        )
            .onEach { output ->
                when (output) {
                    is AddOdometerRecordUseCase.Output.Progress -> updateState { copy(isSaving = true) }
                    is AddOdometerRecordUseCase.Output.Success -> {
                        analytics.track(AnalyticsEvent.Custom("odometer_updated", mapOf("value" to odometerValue)))
                        if (fuelAmount != null) {
                            analytics.track(AnalyticsEvent.Custom("fuel_entry_added", mapOf("amount" to fuelAmount)))
                        }
                        updateState { copy(isSaving = false, showBottomSheet = false, isLoading = false) }
                        
                        // If we were confirming a GPS trip, clear the tracking state
                        if (state.value.newRecordLabel == "Viaje GPS") {
                            trackingRepository.clear()
                        }
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

    /**
     * Orchestrates the calculation of all UI metrics based on the contract and current mileage.
     */
    private fun calculateMetrics(renting: RentingContract, actualKmsDrivenSinceStart: Double) {
        val currentTime = System.currentTimeMillis()

        val totalDays = renting.durationMonths * DAYS_IN_MONTH

        val daysPassed = (currentTime - renting.startDate) / MILLIS_IN_DAY.toDouble()
            .coerceAtLeast(0.0)

        val baseDailyBudget = renting.totalKms / totalDays

        val monthlyBudget = renting.totalKms.toDouble() / renting.durationMonths

        val theoreticalKms = daysPassed * baseDailyBudget

        val balance = theoreticalKms - actualKmsDrivenSinceStart

        val timeUsedPercentage = (daysPassed / totalDays).coerceIn(0.0, 1.0).toFloat()
        val kmsUsedPercentage = (actualKmsDrivenSinceStart / renting.totalKms).coerceIn(0.0, 1.0).toFloat()
        val differencePercentage = ((timeUsedPercentage - kmsUsedPercentage) * 100)

        // Total accumulated odometer
        val currentOdometer = renting.startOdometer + actualKmsDrivenSinceStart
        updateState {
            copy(
                renting = renting,
                balance = balance.toInt(),
                dailyLimit = baseDailyBudget.toInt(),
                monthlyLimit = monthlyBudget.toInt(),
                totalKmsDriven = currentOdometer.toInt(),
                timePercentage = timeUsedPercentage,
                kmsPercentage = kmsUsedPercentage,
                differencePercentage = differencePercentage
            )
        }
        loadMonthlyUsage()
    }

    private fun handleOnRegisterRentingClicked() {
        logger.d("OverviewViewModel", "Effect launched: NavigateToOnboarding")
        launchEffect(Effect.NavigateToOnboarding())
    }

    private fun observeTracking() {
        trackingRepository.isTracking
            .onEach { isTracking -> updateState { copy(isTracking = isTracking) } }
            .launchIn(viewModelScope)

        trackingRepository.currentDistanceMeters
            .onEach { distance -> updateState { copy(trackedDistance = distance) } }
            .launchIn(viewModelScope)

        trackingRepository.startTime
            .onEach { startTime -> updateState { copy(tripStartTime = startTime) } }
            .launchIn(viewModelScope)
    }

    private fun handleStartTracking() {
        analytics.track(AnalyticsEvent.Custom("tracking_started"))
        launchEffect(Effect.StartTrackingService)
    }

    private fun handleStopTracking() {
        analytics.track(AnalyticsEvent.Custom("tracking_stopped", mapOf("distance" to state.value.trackedDistance)))
        launchEffect(Effect.StopTrackingService)
    }

    private fun handleConfirmTrackedTrip() {
        val totalKms = state.value.trackedDistance / 1000.0
        val tripKms = totalKms.toInt()
        
        // Prepare bottom sheet with the actual trip distance (Additive Model)
        updateState {
            copy(
                showBottomSheet = true,
                newOdometerValue = tripKms.toString()
            )
        }
        
        // Reset tracking repository for next trip
        trackingRepository.stopTracking()
    }
}
