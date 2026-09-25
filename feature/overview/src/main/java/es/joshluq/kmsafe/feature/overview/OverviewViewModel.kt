package es.joshluq.kmsafe.feature.overview

import java.util.UUID
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.kmsafe.core.analytics.AnalyticsTracker
import es.joshluq.kmsafe.core.analytics.model.KmAnalyticsEvent
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.core.domain.usecase.SyncStationGeofencesUseCase
import es.joshluq.kmsafe.core.monetization.domain.MonetizationConfig
import es.joshluq.kmsafe.core.ui.util.DateUtils.getContractEndDate
import es.joshluq.kmsafe.core.ui.util.DateUtils.normalizeToUtc00
import es.joshluq.kmsafe.core.ui.util.NumberFormatter
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationPriority
import es.joshluq.kmsafe.domain.model.NotificationStatus
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.TripProjection
import es.joshluq.kmsafe.domain.usecase.AddOdometerRecordUseCase
import es.joshluq.kmsafe.domain.usecase.ClearTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.GetAllContractsUseCase
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.GetMonthlyUsageUseCase
import es.joshluq.kmsafe.domain.usecase.GetOverviewDataUseCase
import es.joshluq.kmsafe.domain.usecase.GetPreferencesUseCase
import es.joshluq.kmsafe.domain.usecase.GetTripProjectionUseCase
import es.joshluq.kmsafe.domain.usecase.MarkNotificationAsReadUseCase
import es.joshluq.kmsafe.domain.usecase.ObserveActiveNotificationsUseCase
import es.joshluq.kmsafe.domain.usecase.ObserveTrackingStateUseCase
import es.joshluq.kmsafe.domain.usecase.ObserveVehicleBluetoothConnectionUseCase
import es.joshluq.kmsafe.domain.usecase.PublishNotificationIfUnreadUseCase
import es.joshluq.kmsafe.domain.usecase.SelectContractUseCase
import es.joshluq.kmsafe.domain.usecase.StartAutoTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StartTripTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StopAutoTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StopTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.StopTripTrackingUseCase
import es.joshluq.kmsafe.domain.usecase.UpdatePreferencesUseCase
import es.joshluq.kmsafe.feature.overview.model.toUiModel
import kotlin.math.absoluteValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
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
    private val observeActiveNotificationsUseCase: ObserveActiveNotificationsUseCase,
    private val markNotificationAsReadUseCase: MarkNotificationAsReadUseCase,
    private val publishNotificationIfUnreadUseCase: PublishNotificationIfUnreadUseCase,
    private val monetizationConfig: MonetizationConfig,
    private val analytics: AnalyticsTracker,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    private var bluetoothJob: Job? = null
    private var previousIsOverLimit: Boolean? = null
    private var bluetoothMissingNotifiedVehicleId: String? = null

    init {
        consolidatedInitialLoad()
        observeTracking()
        observeNotifications()
        startGeofenceSync()
        updateState { copy(adUnitId = monetizationConfig.getOverviewBannerAdUnitId()) }
    }

    private fun startGeofenceSync() {
        syncStationGeofencesUseCase(SyncStationGeofencesUseCase.Input)
            .launchIn(viewModelScope)
    }

    private fun observeNotifications() {
        observeActiveNotificationsUseCase(ObserveActiveNotificationsUseCase.Input())
            .onEach { output ->
                when (output) {
                    is ObserveActiveNotificationsUseCase.Output.Success -> {
                        val mostRelevant = output.notifications.firstOrNull { it.status == NotificationStatus.UNREAD && !it.isRead }
                        updateState { copy(activeNotification = mostRelevant) }
                    }
                }
            }
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
        val monthlyUsageFlow = getMonthlyUsageUseCase(GetMonthlyUsageUseCase.Input)
            .distinctUntilChanged()
        val projectionFlow = getTripProjectionUseCase(GetTripProjectionUseCase.Input)
            .distinctUntilChanged()

        combine(
            entitlementsFlow,
            preferencesFlow,
            overviewDataFlow,
            allContractsFlow,
            monthlyUsageFlow,
            projectionFlow
        ) { flows: Array<Any> ->
            val entitlementsOutput = flows[0] as GetEntitlementsUseCase.Output
            val preferencesOutput = flows[1] as GetPreferencesUseCase.Output
            val overviewOutput = flows[2] as GetOverviewDataUseCase.Output
            val allContractsOutput = flows[3] as GetAllContractsUseCase.Output
            val monthlyUsageOutput = flows[4] as GetMonthlyUsageUseCase.Output
            val projectionOutput = flows[5] as GetTripProjectionUseCase.Output

            var newState = state.value

            // 1. Process Entitlements & Preferences
            if (entitlementsOutput is GetEntitlementsUseCase.Output.Success &&
                preferencesOutput is GetPreferencesUseCase.Output.Success
            ) {
                val entitlements = entitlementsOutput.entitlements
                val prefs = preferencesOutput.preferences
                val isPremiumUser = entitlements.subscriptionLevel == SubscriptionLevel.PREMIUM
                val isTrialable = entitlements.isFeatureTrialable(Feature.AUTO_TRACKING)
                val isAutoTrackEnabled = prefs.autoTrackingEnabled
                val isPromoDismissed = prefs.autoTrackingPromotionDismissed

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
                    showAutoTrackingPromotion = false
                )
            }

            // 2. Process Monthly Usage
            if (monthlyUsageOutput is GetMonthlyUsageUseCase.Output.Success) {
                val uiModels = monthlyUsageOutput.aggregations.map { it.toUiModel() }
                newState = newState.copy(monthlyUsage = uiModels)
            }

            // 3. Process Trip Projection
            val activeContract = if (overviewOutput is GetOverviewDataUseCase.Output.Success) {
                overviewOutput.contract
            } else {
                newState.renting
            }

            val currentProjection = if (projectionOutput is GetTripProjectionUseCase.Output.Success) {
                val proj = projectionOutput.projection
                if (proj != null && activeContract != null && (proj.contractId.isEmpty() || proj.contractId == activeContract.id)) {
                    proj
                } else null
            } else {
                newState.projection
            }

            // 4. Process Contract & Metrics Data
            if (overviewOutput is GetOverviewDataUseCase.Output.Success) {
                val contract = overviewOutput.contract
                val metrics = overviewOutput.metrics
                if (contract != null && metrics != null) {
                    val isPremium = newState.isPremium ?: false
                    val isAutoTrackingEnabled = newState.autoTrackingEnabled
                    val hasBluetooth = contract.bluetoothDeviceAddress != null
                    val showBluetoothSuggestion = isPremium && isAutoTrackingEnabled && !hasBluetooth

                    checkBluetoothMissingNotification(
                        contract = contract,
                        isPremium = isPremium,
                        isAutoTrackingEnabled = isAutoTrackingEnabled
                    )

                    checkProjectionTransitionNotification(
                        contract = contract,
                        currentProjection = currentProjection,
                        preferencesOutput = preferencesOutput
                    )

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
                        projection = currentProjection,
                        statusCapsule = null,
                        isSyncPending = metrics.isSyncPending,
                        isLoading = false
                    )
                } else {
                    newState = newState.copy(isLoading = false, renting = null)
                }
            } else if (overviewOutput is GetOverviewDataUseCase.Output.Progress) {
                if (newState.renting == null && state.value.renting == null) {
                    newState = newState.copy(isLoading = true)
                }
            }

            // 5. Process Available Vehicles
            if (allContractsOutput is GetAllContractsUseCase.Output.Success) {
                newState = newState.copy(availableVehicles = allContractsOutput.contracts.reversed())
            }

            // 7. Atomic State Update
            val oldMac = state.value.renting?.bluetoothDeviceAddress
            val newMac = newState.renting?.bluetoothDeviceAddress

            if (newState != state.value) {
                updateState { newState }
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
            is Event.OnStatusCapsuleClicked -> {
                // Deprecated: StatusCapsule removed from UI, alerts unified in NotificationPill
            }
            Event.OnDismissStatusCapsule -> updateState { copy(statusCapsule = null) }
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
            Event.OnRequestAssistedPermissions -> launchEffect(Effect.NavigateToAssistedPermissions)
            Event.OnNavigateToPreferences -> launchEffect(Effect.NavigateToPreferences)
            is Event.OnPermissionsResult -> handlePermissionsResult(event.granted)
            Event.OnPremiumUpgradeClicked -> {
                analytics.track(KmAnalyticsEvent.Monetization.UpgradeClicked(source = "top_bar"))
                launchEffect(Effect.NavigateToPremiumPaywall)
            }
            Event.OnDismissAutoTrackingPromotion -> {
                analytics.track(KmAnalyticsEvent.Overview.AutoTrackingPromotionDismissed)
                handleDismissPromotion()
            }
            Event.OnAutoTrackingPromotionAccepted -> {
                analytics.track(KmAnalyticsEvent.Overview.AutoTrackingPromotionAccepted)
                handleDismissPromotion()
                launchEffect(Effect.NavigateToPreferences)
            }
            is Event.OnAutoTrackingToggled -> handleAutoTrackingToggled(event.enabled)
            Event.OnDismissBluetoothSuggestionBanner -> {
                updateState { copy(showBluetoothSuggestionBanner = false) }
            }
            Event.OnWelcomeGuideClicked -> launchEffect(Effect.NavigateToWelcomeDiscovery)
            is Event.OnNotificationPillClicked -> {
                val targetNotification = state.value.activeNotification ?: event.notification
                updateState { copy(activeNotification = null) }
                when (targetNotification.topic) {
                    NotificationTopic.PROJECTION -> {
                        launchEffect(Effect.NavigateToProjection)
                    }
                    NotificationTopic.SYSTEM -> {
                        val contractId = targetNotification.data?.get("contract_id") as? String
                            ?: state.value.renting?.id
                        launchEffect(Effect.NavigateToOnboarding(vehicleId = contractId, isEdit = true))
                    }
                    else -> {
                        val deepLink = targetNotification.deepLinkUri
                        if (!deepLink.isNullOrBlank()) {
                            launchEffect(Effect.NavigateToDeepLink(deepLink))
                        } else {
                            launchEffect(Effect.NavigateToNotificationDetail(targetNotification.id))
                        }
                    }
                }
                viewModelScope.launch {
                    runCatching {
                        markNotificationAsReadUseCase(MarkNotificationAsReadUseCase.Input(targetNotification.id))
                    }
                }
            }
            Event.OnViewAllNotificationsClicked -> {
                launchEffect(Effect.NavigateToNotificationsList)
            }
        }
    }

    private fun checkProjectionTransitionNotification(
        contract: RentingContract,
        currentProjection: TripProjection?,
        preferencesOutput: GetPreferencesUseCase.Output
    ) {
        if (currentProjection == null) return

        val currentOverLimit = currentProjection.isOverLimit
        val lastKnown = if (preferencesOutput is GetPreferencesUseCase.Output.Success) {
            preferencesOutput.preferences.lastKnownOverLimit
        } else null

        if (previousIsOverLimit == null) {
            previousIsOverLimit = lastKnown ?: currentOverLimit
        }

        if (previousIsOverLimit != currentOverLimit) {
            val distanceStr = NumberFormatter.formatDistance(currentProjection.expectedFinalBalance.absoluteValue)
            val todayEpochDay = System.currentTimeMillis() / (24 * 60 * 60 * 1000)
            if (currentOverLimit) {
                val notif = Notification(
                    id = UUID.randomUUID().toString(),
                    topic = NotificationTopic.PROJECTION,
                    title = "Alerta de exceso proyectado",
                    body = "Tu ritmo actual proyecta superar el límite contratado en $distanceStr km.",
                    priority = NotificationPriority.CRITICAL,
                    status = NotificationStatus.UNREAD,
                    deepLinkUri = "kmsafe://feature/projection",
                    actionLabel = "Ver Proyección",
                    data = mapOf(
                        "projection_key" to "proj_${contract.id}_$todayEpochDay",
                        "contract_id" to contract.id,
                        "current_km" to currentProjection.expectedFinalBalance.absoluteValue,
                        "excess_km" to distanceStr,
                        "is_over_limit" to true
                    )
                )
                viewModelScope.launch {
                    publishNotificationIfUnreadUseCase(PublishNotificationIfUnreadUseCase.Input(notif))
                }
            } else {
                val notif = Notification(
                    id = UUID.randomUUID().toString(),
                    topic = NotificationTopic.PROJECTION,
                    title = "Ritmo de kilometraje recuperado",
                    body = "Tu proyección actual está dentro de los límites del contrato.",
                    priority = NotificationPriority.INFO,
                    status = NotificationStatus.UNREAD,
                    deepLinkUri = "kmsafe://feature/projection",
                    data = mapOf(
                        "projection_key" to "proj_safe_${contract.id}_$todayEpochDay",
                        "contract_id" to contract.id,
                        "is_over_limit" to false
                    )
                )
                viewModelScope.launch {
                    publishNotificationIfUnreadUseCase(PublishNotificationIfUnreadUseCase.Input(notif))
                }
            }
            previousIsOverLimit = currentOverLimit
            updatePreferencesUseCase(
                UpdatePreferencesUseCase.Input(lastKnownOverLimit = currentOverLimit)
            ).launchIn(viewModelScope)
        }
    }

    private fun checkBluetoothMissingNotification(
        contract: RentingContract,
        isPremium: Boolean,
        isAutoTrackingEnabled: Boolean
    ) {
        val hasBluetooth = contract.bluetoothDeviceAddress != null
        val semanticKey = "bt_missing_${contract.id}"

        if (isPremium && isAutoTrackingEnabled && !hasBluetooth) {
            if (bluetoothMissingNotifiedVehicleId != contract.id) {
                bluetoothMissingNotifiedVehicleId = contract.id
                val notif = Notification(
                    id = UUID.randomUUID().toString(),
                    topic = NotificationTopic.SYSTEM,
                    title = "Dispositivo Bluetooth no configurado",
                    body = "Configura el Bluetooth de tu vehículo para habilitar el auto-tracking inteligente.",
                    priority = NotificationPriority.WARNING,
                    status = NotificationStatus.UNREAD,
                    deepLinkUri = "kmsafe://feature/fleet/edit?vehicleId=${contract.id}",
                    actionLabel = "Configurar",
                    data = mapOf(
                        "deduplication_key" to semanticKey,
                        "contract_id" to contract.id
                    )
                )
                viewModelScope.launch {
                    publishNotificationIfUnreadUseCase(PublishNotificationIfUnreadUseCase.Input(notif))
                }
            }
        } else if (hasBluetooth) {
            if (bluetoothMissingNotifiedVehicleId == contract.id) {
                bluetoothMissingNotifiedVehicleId = null
                viewModelScope.launch {
                    markNotificationAsReadUseCase(MarkNotificationAsReadUseCase.Input(semanticKey))
                }
            }
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
        val targetVehicleName = state.value.availableVehicles.find { it.id == id }?.vehicleName
        updateState {
            copy(
                showVehicleSwitcher = false,
                isSwitchingVehicle = true,
                switchingVehicleName = targetVehicleName,
                isLoading = true
            )
        }
        selectContractUseCase(SelectContractUseCase.Input(id = id, targetVehicleName = targetVehicleName))
            .onEach { output ->
                when (output) {
                    is SelectContractUseCase.Output.Success -> {
                        updateState {
                            copy(
                                isLoading = false,
                                isSwitchingVehicle = false,
                                switchingVehicleName = null
                            )
                        }
                    }
                    is SelectContractUseCase.Output.Progress -> {
                        updateState { copy(isLoading = true, isSwitchingVehicle = true) }
                    }
                    is SelectContractUseCase.Output.Failure -> {
                        updateState {
                            copy(
                                isLoading = false,
                                isSwitchingVehicle = false,
                                switchingVehicleName = null
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
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
                        analytics.track(KmAnalyticsEvent.Overview.OdometerUpdated(odometerValue))
                        if (fuelAmount != null) {
                            analytics.track(KmAnalyticsEvent.Overview.FuelEntryAdded(fuelAmount))
                        }
                        updateState { copy(isSaving = false, showBottomSheet = false, isLoading = false) }
                        clearTrackingUseCase(ClearTrackingUseCase.Input).launchIn(viewModelScope)
                        launchEffect(Effect.DismissTrackingNotifications)
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
                    val shouldSuppressTrackingUi = state.value.showBottomSheet || state.value.isSaving
                    updateState {
                        copy(
                            isTracking = if (shouldSuppressTrackingUi) false else output.isTracking,
                            trackedDistance = if (shouldSuppressTrackingUi && isTracking) trackedDistance else output.trackedDistance,
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
        analytics.track(KmAnalyticsEvent.Tracking.TrackingStarted)
        viewModelScope.launch {
            startTripTrackingUseCase(StartTripTrackingUseCase.Input)
        }
    }

    private fun handleStopTracking() {
        logger.d("OverviewViewModel", "handleStopTracking called")
        analytics.track(KmAnalyticsEvent.Tracking.TrackingStopped(state.value.trackedDistance))
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
        viewModelScope.launch {
            stopTripTrackingUseCase(StopTripTrackingUseCase.Input)
            stopTrackingUseCase(StopTrackingUseCase.Input).collect()
        }
    }

    private fun handleCancelTrackedTrip() {
        analytics.track(KmAnalyticsEvent.Tracking.TrackingCancelled)
        viewModelScope.launch {
            stopTripTrackingUseCase(StopTripTrackingUseCase.Input)
            clearTrackingUseCase(ClearTrackingUseCase.Input).collect()
        }
        launchEffect(Effect.DismissTrackingNotifications)
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

                    // Re-trigger evaluation of the banner and notifications after toggle
                    val isPremium = state.value.isPremium ?: false
                    val hasBluetooth = state.value.renting?.bluetoothDeviceAddress != null
                    val showBluetooth = isPremium && enabled && !hasBluetooth
                    state.value.renting?.let { contract ->
                        checkBluetoothMissingNotification(contract, isPremium, enabled)
                    }
                    updateState {
                        copy(
                            showBluetoothSuggestionBanner = showBluetooth,
                            statusCapsule = null
                        )
                    }
                }
            }.launchIn(viewModelScope)
    }
}
