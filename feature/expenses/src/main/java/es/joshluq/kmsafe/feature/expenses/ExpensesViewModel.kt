package es.joshluq.kmsafe.feature.expenses

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.core.monetization.domain.MonetizationConfig
import es.joshluq.kmsafe.domain.model.EnergyCategory
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.usecase.CalculateCostPerHundredKmUseCase
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteFuelExpenseUseCase
import es.joshluq.kmsafe.domain.usecase.DiscardReceiptScanUseCase
import es.joshluq.kmsafe.domain.usecase.GetAllServiceStationsUseCase
import es.joshluq.kmsafe.domain.usecase.GetElectrificationSavingsUseCase
import es.joshluq.kmsafe.domain.usecase.GetExpensesByVehicleUseCase
import es.joshluq.kmsafe.domain.usecase.GetStationVolatilityUseCase
import es.joshluq.kmsafe.domain.usecase.ObserveStationRadarUseCase
import es.joshluq.kmsafe.domain.usecase.ProcessFuelReceiptUseCase
import es.joshluq.kmsafe.domain.usecase.SaveFuelExpenseUseCase
import es.joshluq.kmsafe.domain.usecase.SaveServiceStationUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * MVI ViewModel for managing vehicle fuel and electric expenses.
 */
@HiltViewModel(assistedFactory = ExpensesViewModel.Factory::class)
class ExpensesViewModel @AssistedInject constructor(
    @Assisted("stationId") val initialStationId: String?,
    @Assisted("autoOpenAdd") val autoOpenAdd: Boolean,
    @Assisted("priceReportMode") val priceReportMode: Boolean,
    private val getExpensesByVehicleUseCase: GetExpensesByVehicleUseCase,
    private val saveFuelExpenseUseCase: SaveFuelExpenseUseCase,
    private val deleteFuelExpenseUseCase: DeleteFuelExpenseUseCase,
    private val getStationVolatilityUseCase: GetStationVolatilityUseCase,
    private val getElectrificationSavingsUseCase: GetElectrificationSavingsUseCase,
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
    private val getAllServiceStationsUseCase: GetAllServiceStationsUseCase,
    private val saveServiceStationUseCase: SaveServiceStationUseCase,
    private val calculateCostPerHundredKmUseCase: CalculateCostPerHundredKmUseCase,
    private val observeStationRadarUseCase: ObserveStationRadarUseCase,
    private val processFuelReceiptUseCase: ProcessFuelReceiptUseCase,
    private val discardReceiptScanUseCase: DiscardReceiptScanUseCase,
    private val monetizationConfig: MonetizationConfig,
    private val logger: LoggerKit
) : ScreenViewModel<ExpensesState, ExpensesEvent, ExpensesEffect>() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("stationId") stationId: String?,
            @Assisted("autoOpenAdd") autoOpenAdd: Boolean,
            @Assisted("priceReportMode") priceReportMode: Boolean
        ): ExpensesViewModel
    }

    init {
        checkSubscription()
        loadExpenses()
        loadStations()
        if (autoOpenAdd) {
            updateState { 
                copy(
                    isAddExpenseSheetOpen = true,
                    initialStationId = initialStationId,
                    priceReportMode = priceReportMode,
                    isStationAutoDetected = initialStationId != null
                ) 
            }
        }
        updateState { copy(adUnitId = monetizationConfig.getExpensesBannerAdUnitId()) }
    }

    override fun createInitialState(): ExpensesState = ExpensesState.Empty

    override fun handleEvent(event: ExpensesEvent) {
        logger.d("ExpensesViewModel", "Handling event: $event")
        when (event) {
            ExpensesEvent.OnRefresh -> loadExpenses()
            is ExpensesEvent.OnFilterChanged -> handleFilterChanged(event.mode)
            ExpensesEvent.OnOpenAddExpense -> updateState { copy(isAddExpenseSheetOpen = true) }
            ExpensesEvent.OnDismissAddExpense -> {
                handleDiscardReceiptScan()
                updateState {
                    copy(
                        isAddExpenseSheetOpen = false,
                        currentLat = null,
                        currentLng = null,
                        initialStationId = null,
                        priceReportMode = false,
                        isStationAutoDetected = false
                    )
                }
            }
            is ExpensesEvent.OnLocationCaptured -> updateState { copy(currentLat = event.latitude, currentLng = event.longitude) }
            ExpensesEvent.OnManageStationsClicked -> launchEffect(ExpensesEffect.NavigateToStations)
            is ExpensesEvent.OnDeleteExpense -> updateState { copy(showDeleteConfirmation = true, deleteTargetId = event.expenseId) }
            ExpensesEvent.OnConfirmDeleteExpense -> {
                val id = state.value.deleteTargetId
                if (id != null) handleDeleteExpense(id)
                updateState { copy(showDeleteConfirmation = false, deleteTargetId = null) }
            }
            ExpensesEvent.OnCancelDeleteExpense -> updateState { copy(showDeleteConfirmation = false, deleteTargetId = null) }
            is ExpensesEvent.OnSaveExpense -> handleSaveExpense(event)
            is ExpensesEvent.OnViewStationVolatility -> handleViewVolatility(event.stationId, event.fuelType)
            is ExpensesEvent.OnViewStationDetail -> launchEffect(ExpensesEffect.NavigateToStationDetail(event.stationId))
            ExpensesEvent.OnDismissVolatility -> updateState { copy(selectedVolatility = null) }
            ExpensesEvent.OnDismissError -> updateState { copy(error = null) }
            ExpensesEvent.OnDismissSuccess -> updateState { copy(successMessage = null) }
            ExpensesEvent.OnDismissConsumptionBanner -> updateState { copy(consumptionBannerData = null) }
            ExpensesEvent.OnToggleTripsVisibility -> updateState { copy(showTripsSinceLastRefuel = !showTripsSinceLastRefuel) }
            is ExpensesEvent.OnStationRadarSelected -> {
                updateState {
                    copy(
                        isAddExpenseSheetOpen = true,
                        initialStationId = event.stationId
                    )
                }
            }
            ExpensesEvent.OnUpgradeToUnlockRadarClicked -> launchEffect(ExpensesEffect.NavigateToUpgrade)
            is ExpensesEvent.OnReceiptUriSelected -> handleReceiptUriSelected(event.uri)
            is ExpensesEvent.OnReceiptImageCaptured -> handleReceiptUriSelected(event.imagePath.toUri())
            ExpensesEvent.OnDiscardReceiptScan -> handleDiscardReceiptScan()
        }
    }

    private fun checkSubscription() {
        checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.ADVANCED_PROJECTIONS))
            .onEach { output ->
                if (output is CheckFeatureAccessUseCase.Output.Success) {
                    updateState { copy(isPremium = output.isGranted) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadExpenses() {
        getExpensesByVehicleUseCase(GetExpensesByVehicleUseCase.Input())
            .onEach { output ->
                when (output) {
                    GetExpensesByVehicleUseCase.Output.Progress -> {
                        updateState { copy(isLoading = true) }
                    }
                    is GetExpensesByVehicleUseCase.Output.Empty -> {
                        updateState {
                            copy(
                                vehicleId = output.vehicleId,
                                vehicleName = output.vehicleName,
                                isLoading = false,
                                expenses = emptyList(),
                                filteredExpenses = emptyList(),
                                currentMonthTotalCost = 0.0,
                                currentMonthTotalVolume = 0.0,
                                allTimeTotalCost = 0.0,
                                currentOdometer = output.currentOdometer,
                                lastRefuelTimestamp = output.lastRefuelTimestamp,
                                kmSinceLastFullRefuel = output.kmSinceLastFullRefuel,
                                recordsSinceLastRefuel = output.recordsSinceLastRefuel,
                                vehicleFuelType = output.defaultFuelType,
                                lastUsedFuelType = output.defaultFuelType
                            )
                        }
                        loadCostPerHundredKm(output.vehicleId)
                        loadStationRadar(output.vehicleId)
                    }
                    is GetExpensesByVehicleUseCase.Output.Success -> {
                        updateState {
                            val filtered = filterExpenses(output.expenses, filterMode)
                            // Use the most recent expense to derive the fuel type, 
                            // or fallback to the vehicle's default fuel type from the contract.
                            val mostRecent = output.expenses.firstOrNull()
                            val derivedFuelType = mostRecent?.fuelType ?: output.defaultFuelType
                            // Pre-fill price from last expense with the same fuel type
                            val lastPrice = output.expenses
                                .filter { it.fuelType == derivedFuelType }
                                .maxByOrNull { it.timestamp }
                                ?.unitPrice

                            val lastGasPrice = output.expenses
                                .filter { it.fuelType.category == EnergyCategory.COMBUSTION }
                                .maxByOrNull { it.timestamp }
                                ?.unitPrice

                            val lastElecPrice = output.expenses
                                .filter { it.fuelType.category == EnergyCategory.ELECTRIC }
                                .maxByOrNull { it.timestamp }
                                ?.unitPrice

                            copy(
                                isLoading = false,
                                vehicleId = output.vehicleId,
                                vehicleName = output.vehicleName,
                                expenses = output.expenses,
                                filteredExpenses = filtered,
                                currentMonthTotalCost = output.currentMonthTotalCost,
                                currentMonthTotalVolume = output.currentMonthTotalVolume,
                                allTimeTotalCost = output.allTimeTotalCost,
                                currentOdometer = output.currentOdometer,
                                lastRefuelTimestamp = output.lastRefuelTimestamp,
                                kmSinceLastFullRefuel = output.kmSinceLastFullRefuel,
                                recordsSinceLastRefuel = output.recordsSinceLastRefuel,
                                vehicleFuelType = output.defaultFuelType,
                                lastUsedFuelType = derivedFuelType,
                                lastUnitPrice = lastPrice,
                                lastGasolinePrice = lastGasPrice,
                                lastElectricPrice = lastElecPrice
                            )
                        }
                        loadElectrificationSavings(output.vehicleId)
                        loadCostPerHundredKm(output.vehicleId)
                        loadStationRadar(output.vehicleId)
                    }
                    is GetExpensesByVehicleUseCase.Output.Failure -> {
                        updateState { copy(isLoading = false, error = output.error.toText()) }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadStations() {
        getAllServiceStationsUseCase(GetAllServiceStationsUseCase.Input)
            .onEach { output ->
                if (output is GetAllServiceStationsUseCase.Output.Success) {
                    updateState { copy(stations = output.stations) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadElectrificationSavings(vehicleId: String) {
        getElectrificationSavingsUseCase(GetElectrificationSavingsUseCase.Input(vehicleId))
            .onEach { output ->
                if (output is GetElectrificationSavingsUseCase.Output.Success) {
                    updateState { copy(electrificationSavingsEuros = output.totalSavedEuros) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadCostPerHundredKm(vehicleId: String) {
        calculateCostPerHundredKmUseCase(CalculateCostPerHundredKmUseCase.Input(vehicleId))
            .onEach { output ->
                when (output) {
                    is CalculateCostPerHundredKmUseCase.Output.Success -> {
                        updateState {
                            copy(
                                costPer100km = output.costPer100km,
                                lastCycleConsumption = output.lastCycleConsumption,
                                averageConsumption = output.averageConsumption,
                                consumptionDeltaVsAverage = output.consumptionDeltaVsAverage
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadStationRadar(vehicleId: String) {
        observeStationRadarUseCase(ObserveStationRadarUseCase.Input(vehicleId))
            .onEach { output ->
                when (output) {
                    is ObserveStationRadarUseCase.Output.Success -> {
                        updateState {
                            copy(
                                radarItems = output.items,
                                isRadarLocked = output.isLocked
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleFilterChanged(mode: ExpenseFilterMode) {
        updateState {
            copy(
                filterMode = mode,
                filteredExpenses = filterExpenses(expenses, mode)
            )
        }
    }

    private fun filterExpenses(
        expenses: List<FuelExpense>,
        mode: ExpenseFilterMode
    ): List<FuelExpense> {
        return when (mode) {
            ExpenseFilterMode.ALL -> expenses
            ExpenseFilterMode.COMBUSTION -> expenses.filter { it.fuelType.category == EnergyCategory.COMBUSTION }
            ExpenseFilterMode.ELECTRIC -> expenses.filter { it.fuelType.category == EnergyCategory.ELECTRIC }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun handleSaveExpense(event: ExpensesEvent.OnSaveExpense) {
        logger.d("ExpensesViewModel", "handleSaveExpense triggered. isSaving=${state.value.isSaving}")
        if (state.value.isSaving) {
            logger.w("ExpensesViewModel", "Save ignored: already in progress")
            return
        }

        val currentVehicleId = state.value.vehicleId
        if (currentVehicleId == null) {
            logger.e("ExpensesViewModel", "Save failed: no active vehicle")
            updateState { copy(error = TextProvider.Resource(R.string.expenses_error_no_active_vehicle)) }
            return
        }

        updateState { copy(isSaving = true) }
        logger.d("ExpensesViewModel", "Starting save flow for vehicle: $currentVehicleId")

        val trimmedStationName = event.stationName?.trim()
        val matchingStation = if (event.stationId == null && !trimmedStationName.isNullOrEmpty()) {
            state.value.stations.firstOrNull { station ->
                station.name.trim().equals(trimmedStationName, ignoreCase = true) ||
                station.brand.trim().equals(trimmedStationName, ignoreCase = true)
            }
        } else {
            null
        }

        val resolvedStationId = event.stationId ?: matchingStation?.id

        // Logic to handle station: if resolved ID exists, reuse it; otherwise, create/save station if name is provided
        val stationFlow = if (resolvedStationId == null && !trimmedStationName.isNullOrEmpty()) {
            logger.d("ExpensesViewModel", "Station not found in existing list, creating/saving station: $trimmedStationName")
            saveServiceStationUseCase(
                SaveServiceStationUseCase.Input(
                    name = trimmedStationName,
                    brand = trimmedStationName, // Use name as brand for new manual entries
                    latitude = state.value.currentLat ?: 0.0,
                    longitude = state.value.currentLng ?: 0.0,
                    address = "",
                    availableEnergies = listOf(event.fuelType)
                )
            ).flatMapLatest { output ->
                logger.d("ExpensesViewModel", "SaveServiceStation output received: $output")
                when (output) {
                    is SaveServiceStationUseCase.Output.Success -> {
                        flowOf(output.stationId)
                    }

                    is SaveServiceStationUseCase.Output.Failure -> {
                        // Fallback to null ID if station save fails, but log it
                        logger.e("ExpensesViewModel", "Station save failed, proceeding with null ID")
                        flowOf(null)
                    }

                    else -> {
                        emptyFlow()
                    }
                }
            }
        } else {
            logger.d("ExpensesViewModel", "Using existing or matched station ID: $resolvedStationId")
            flowOf(resolvedStationId)
        }

        stationFlow.flatMapLatest { finalStationId ->
            logger.d("ExpensesViewModel", "Proceeding to SaveFuelExpense with stationId: $finalStationId")
            val input = SaveFuelExpenseUseCase.Input(
                vehicleId = currentVehicleId,
                fuelType = event.fuelType,
                unitPrice = event.unitPrice,
                volumeQuantity = event.volumeQuantity,
                totalCost = event.totalCost,
                stationId = finalStationId,
                stationName = event.stationName,
                odometerAtExpense = event.odometerAtExpense,
                isFullTank = event.isFullTank,
                notes = event.notes,
                lastRefuelTimestamp = event.lastRefuelTimestamp,
                receiptImagePath = event.receiptImagePath ?: state.value.receiptImagePath
            )
            saveFuelExpenseUseCase(input)
        }.onEach { output ->
            logger.d("ExpensesViewModel", "SaveFuelExpense output received: $output")
            when (output) {
                is SaveFuelExpenseUseCase.Output.Success -> {
                    logger.i("ExpensesViewModel", "Expense saved successfully: ${output.expenseId}")
                    updateState {
                        copy(
                            isAddExpenseSheetOpen = false,
                            isSaving = false,
                            isScanningReceipt = false,
                            scannedReceiptResult = null,
                            receiptImagePath = null
                        )
                    }
                    // Reload stations to include the new one if created
                    loadStations()
                    
                    // Show enriched consumption banner for full-tank refuels
                    if (event.isFullTank) {
                        val savedExpense = state.value.expenses
                            .firstOrNull { it.id == output.expenseId }
                        val consumption = savedExpense?.consumptionPer100km
                        if (consumption != null) {
                            val unit = event.fuelType.unitOfMeasure
                            val average = state.value.expenses
                                .filter { it.isFullTank && it.consumptionPer100km != null && it.fuelType == event.fuelType }
                                .mapNotNull { it.consumptionPer100km }
                                .takeIf { it.isNotEmpty() }
                                ?.average()
                            updateState {
                                copy(consumptionBannerData = ConsumptionBannerData(consumption, unit, average))
                            }
                        }
                    }
                }
                is SaveFuelExpenseUseCase.Output.InvalidInput -> {
                    logger.w("ExpensesViewModel", "Save failed: Invalid input - ${output.error}")
                    updateState { copy(error = output.error.toText(), isSaving = false) }
                }
                is SaveFuelExpenseUseCase.Output.Failure -> {
                    logger.e("ExpensesViewModel", "Save failed: UseCase failure - ${output.error}")
                    updateState { copy(error = output.error.toText(), isSaving = false) }
                }
                SaveFuelExpenseUseCase.Output.Progress -> {
                    logger.d("ExpensesViewModel", "Save in progress...")
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun handleReceiptUriSelected(uri: Uri) {
        val vehicleId = state.value.vehicleId
        if (vehicleId == null) {
            updateState { copy(error = TextProvider.Resource(R.string.expenses_error_no_active_vehicle)) }
            return
        }

        if (state.value.isPremium != true) {
            logger.w("ExpensesViewModel", "Receipt scan blocked: user is not premium")
            updateState { copy(error = KmError.FuelExpensesPremiumOnly.toText()) }
            launchEffect(ExpensesEffect.NavigateToUpgrade)
            return
        }

        updateState { copy(isScanningReceipt = true) }

        processFuelReceiptUseCase(
            ProcessFuelReceiptUseCase.Input(
                uriPath = uri.toString(),
                vehicleId = vehicleId
            )
        ).onEach { output ->
            when (output) {
                ProcessFuelReceiptUseCase.Output.Progress -> {
                    updateState { copy(isScanningReceipt = true) }
                }
                is ProcessFuelReceiptUseCase.Output.Success -> {
                    updateState {
                        copy(
                            isScanningReceipt = false,
                            scannedReceiptResult = output.result,
                            receiptImagePath = uri.toString(),
                            isAddExpenseSheetOpen = true
                        )
                    }
                }
                is ProcessFuelReceiptUseCase.Output.Failure -> {
                    updateState {
                        copy(
                            isScanningReceipt = false,
                            error = output.error.toText()
                        )
                    }
                    if (output.error is KmError.FuelExpensesPremiumOnly) {
                        launchEffect(ExpensesEffect.NavigateToUpgrade)
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun handleDiscardReceiptScan() {
        val path = state.value.scannedReceiptResult?.storageFilePath
        if (path != null) {
            discardReceiptScanUseCase(DiscardReceiptScanUseCase.Input(filePath = path))
                .launchIn(viewModelScope)
        }
        updateState {
            copy(
                scannedReceiptResult = null,
                receiptImagePath = null,
                isScanningReceipt = false
            )
        }
    }

    private fun handleDeleteExpense(expenseId: String) {
        deleteFuelExpenseUseCase(DeleteFuelExpenseUseCase.Input(expenseId))
            .onEach { output ->
                when (output) {
                    is DeleteFuelExpenseUseCase.Output.Success -> {
                        updateState { copy(successMessage = TextProvider.Resource(R.string.expenses_success_deleted)) }
                    }
                    is DeleteFuelExpenseUseCase.Output.Failure -> {
                        updateState { copy(error = output.error.toText()) }
                    }
                    DeleteFuelExpenseUseCase.Output.Progress -> {}
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleViewVolatility(stationId: String, fuelType: FuelType) {
        getStationVolatilityUseCase(GetStationVolatilityUseCase.Input(stationId, fuelType))
            .onEach { output ->
                if (output is GetStationVolatilityUseCase.Output.Success) {
                    updateState { copy(selectedVolatility = output.volatility) }
                }
            }
            .launchIn(viewModelScope)
    }
}
