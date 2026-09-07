package es.joshluq.kmsafe.feature.fleet.setup

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.core.ui.util.toText
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.SyncStatus
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.GetAllContractsUseCase
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.GetImageBytesUseCase
import es.joshluq.kmsafe.domain.usecase.SaveInitialContractUseCase
import es.joshluq.kmsafe.domain.usecase.UploadVehicleImageUseCase
import es.joshluq.kmsafe.feature.fleet.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SetupWizardViewModel @Inject constructor(
    private val saveInitialContractUseCase: SaveInitialContractUseCase,
    private val getEntitlementsUseCase: GetEntitlementsUseCase,
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
    private val getAllContractsUseCase: GetAllContractsUseCase,
    private val uploadVehicleImage: UploadVehicleImageUseCase,
    private val getImageBytesUseCase: GetImageBytesUseCase,
    private val analytics: AnalyticskitManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    private val temporaryContractId = UUID.randomUUID().toString()

    init {
        loadEntitlements()
        checkMultiVehicleEligibility()
    }

    override fun createInitialState(): State = State()

    override fun handleEvent(event: Event) {
        when (event) {
            Event.OnNextClicked -> handleNext()
            Event.OnBackClicked -> handleBack()
            Event.OnSkipStepClicked -> handleSkip()

            is Event.OnVehicleNameChanged -> updateState { copy(vehicleName = event.value, vehicleNameError = null) }
            is Event.OnFuelTypeChanged -> updateState { copy(fuelType = event.value) }
            is Event.OnOriginalImageSelected -> {
                event.uri?.let { launchEffect(Effect.NavigateToCropper(it.toString())) }
            }
            is Event.OnImageSelected -> updateState { copy(selectedImageUri = event.uri) }
            is Event.OnStartDateChanged -> updateState { copy(startDate = event.value, startDateError = null) }
            is Event.OnDurationMonthsChanged -> updateState {
                copy(
                    durationMonths = event.value,
                    durationMonthsError = null
                )
            }
            is Event.OnTotalKmsChanged -> updateState {
                copy(totalKms = event.value, totalKmsError = null, showMileageWarning = false)
            }
            is Event.OnStartOdometerChanged -> updateState {
                copy(
                    startOdometer = event.value,
                    startOdometerError = null,
                    showMileageWarning = false
                )
            }
            is Event.OnCurrentOdometerChanged -> updateState {
                copy(
                    currentOdometer = event.value,
                    currentOdometerError = null,
                    showMileageWarning = false
                )
            }
            is Event.OnBluetoothDeviceSelected -> updateState {
                copy(
                    bluetoothDeviceName = event.name,
                    bluetoothDeviceAddress = event.address,
                    showBluetoothPicker = false
                )
            }
            is Event.OnExcessDistancePriceChanged -> updateState {
                copy(
                    excessDistancePrice = event.value,
                    excessDistancePriceError = null
                )
            }
            is Event.OnCourtesyMarginKmsChanged -> updateState {
                copy(
                    courtesyMarginKms = event.value,
                    courtesyMarginError = null
                )
            }

            Event.OnToggleBluetoothPicker -> {
                val newState = !state.value.showBluetoothPicker
                updateState { copy(showBluetoothPicker = newState) }
            }
            Event.OnDismissError -> updateState { copy(error = null) }
            Event.OnUpgradeClicked -> {
                updateState { copy(showPremiumLimit = false) }
                launchEffect(Effect.NavigateToPremiumPaywall)
            }
            Event.OnDismissPremiumLimit -> {
                updateState { copy(showPremiumLimit = false) }
                launchEffect(Effect.NavigateBack)
            }
        }
    }

    private fun handleNext() {
        when (state.value.currentStep) {
            SetupStep.VEHICLE_IDENTITY -> {
                if (validateIdentity()) {
                    updateState { copy(currentStep = SetupStep.CONTRACT_TIMEFRAME) }
                }
            }
            SetupStep.CONTRACT_TIMEFRAME -> {
                if (validateTimeframe()) {
                    updateState { copy(currentStep = SetupStep.MILEAGE_BUDGET) }
                }
            }
            SetupStep.MILEAGE_BUDGET -> {
                if (validateMileage()) {
                    updateState { copy(currentStep = SetupStep.SMART_ACTIVATION) }
                }
            }
            SetupStep.SMART_ACTIVATION -> {
                if (validateBluetooth()) {
                    updateState { copy(currentStep = SetupStep.ADVANCED_PROTECTION) }
                }
            }
            SetupStep.ADVANCED_PROTECTION -> {
                if (validateAdvanced()) {
                    saveContract()
                }
            }
        }
    }

    private fun handleBack() {
        val prevStep = when (state.value.currentStep) {
            SetupStep.VEHICLE_IDENTITY -> null
            SetupStep.CONTRACT_TIMEFRAME -> SetupStep.VEHICLE_IDENTITY
            SetupStep.MILEAGE_BUDGET -> SetupStep.CONTRACT_TIMEFRAME
            SetupStep.SMART_ACTIVATION -> SetupStep.MILEAGE_BUDGET
            SetupStep.ADVANCED_PROTECTION -> SetupStep.SMART_ACTIVATION
        }

        if (prevStep != null) {
            updateState { copy(currentStep = prevStep) }
        } else {
            launchEffect(Effect.NavigateBack)
        }
    }

    private fun handleSkip() {
        when (state.value.currentStep) {
            SetupStep.SMART_ACTIVATION -> updateState { copy(currentStep = SetupStep.ADVANCED_PROTECTION) }
            SetupStep.ADVANCED_PROTECTION -> saveContract()
            else -> handleNext()
        }
    }

    private fun validateIdentity(): Boolean {
        return if (state.value.vehicleName.isBlank()) {
            updateState { copy(vehicleNameError = TextProvider.Resource(R.string.onboarding_vehicle_name_feedback)) }
            false
        } else {
            true
        }
    }

    private fun validateTimeframe(): Boolean {
        val s = state.value
        var isValid = true
        if (s.startDate.isBlank()) {
            updateState { copy(startDateError = TextProvider.Resource(R.string.onboarding_date_feedback)) }
            isValid = false
        }
        val duration = s.durationMonths.toIntOrNull()
        if (duration == null || duration <= 0) {
            updateState { copy(durationMonthsError = TextProvider.Resource(R.string.onboarding_number_feedback)) }
            isValid = false
        }
        return isValid
    }

    private fun validateMileage(): Boolean {
        val s = state.value
        var isValid = true

        val totalKms = s.totalKms.replace(',', '.').toDoubleOrNull()
        if (totalKms == null || totalKms <= 0) {
            updateState { copy(totalKmsError = TextProvider.Resource(R.string.onboarding_number_feedback)) }
            isValid = false
        }

        val startOdo = s.startOdometer.replace(',', '.').toDoubleOrNull()
        if (startOdo == null || startOdo < 0) {
            updateState { copy(startOdometerError = TextProvider.Resource(R.string.onboarding_number_feedback)) }
            isValid = false
        }

        val currentOdo = s.currentOdometer.replace(',', '.').toDoubleOrNull()
        if (currentOdo == null || currentOdo < 0) {
            updateState { copy(currentOdometerError = TextProvider.Resource(R.string.onboarding_number_feedback)) }
            isValid = false
        } else if (startOdo != null && currentOdo < startOdo) {
            updateState {
                copy(
                    currentOdometerError = TextProvider.Resource(R.string.onboarding_current_odometer_feedback)
                )
            }
            isValid = false
        }

        // Additional check: driven distance > total contract mileage
        if (isValid) {
            val driven = currentOdo!! - startOdo!!
            val limit = totalKms!!
            if (driven > limit && !s.showMileageWarning) {
                updateState {
                    copy(
                        showMileageWarning = true,
                        mileageWarningMessage = TextProvider.Resource(
                            R.string.onboarding_mileage_warning,
                            driven.toString(),
                            limit.toString()
                        )
                    )
                }
                return false // Stop and show warning
            }
        }

        return isValid
    }

    private fun validateBluetooth(): Boolean {
        return if (state.value.bluetoothDeviceAddress == null) {
            updateState { copy(error = TextProvider.Resource(R.string.setup_wizard_error_bluetooth_required)) }
            false
        } else {
            true
        }
    }

    private fun validateAdvanced(): Boolean {
        val s = state.value
        var isValid = true

        val price = s.excessDistancePrice.replace(',', '.').toDoubleOrNull()
        if (price == null || price < 0) {
            updateState { copy(excessDistancePriceError = TextProvider.Resource(R.string.onboarding_number_feedback)) }
            isValid = false
        }

        val margin = s.courtesyMarginKms.replace(',', '.').toDoubleOrNull()
        if (margin == null || margin < 0) {
            updateState { copy(courtesyMarginError = TextProvider.Resource(R.string.onboarding_number_feedback)) }
            isValid = false
        }

        return isValid
    }

    private fun loadEntitlements() {
        getEntitlementsUseCase(GetEntitlementsUseCase.Input("", forceRefresh = false))
            .onEach { output ->
                if (output is GetEntitlementsUseCase.Output.Success) {
                    updateState { copy(subscriptionLevel = output.entitlements.subscriptionLevel) }
                }
            }.launchIn(viewModelScope)
    }

    private fun checkMultiVehicleEligibility() {
        combine(
            checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.MULTI_VEHICLE)),
            getAllContractsUseCase(GetAllContractsUseCase.Input)
        ) { accessOutput, contractsOutput ->
            val isMultiVehicleGranted = (accessOutput as? CheckFeatureAccessUseCase.Output.Success)?.isGranted ?: false
            val contracts = (contractsOutput as? GetAllContractsUseCase.Output.Success)?.contracts ?: emptyList()
            Pair(isMultiVehicleGranted, contracts.isNotEmpty())
        }.onEach { (isMultiVehicleGranted, hasExisting) ->
            updateState {
                copy(
                    isMultiVehicleAllowed = isMultiVehicleGranted,
                    hasExistingVehicles = hasExisting,
                    showPremiumLimit = !isMultiVehicleGranted && hasExisting
                )
            }
            if (!isMultiVehicleGranted && hasExisting) {
                logger.w("SetupWizardViewModel", "Multi-vehicle limit reached for free user")
                analytics.track(AnalyticsEvent.Custom("multi_vehicle_limit_reached"))
            }
        }.launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun saveContract() {
        if (!state.value.isMultiVehicleAllowed && state.value.hasExistingVehicles) {
            logger.w("SetupWizardViewModel", "Blocking saveContract: multi-vehicle limit reached")
            updateState { copy(showPremiumLimit = true) }
            return
        }
        val s = state.value
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val startTime = runCatching {
            sdf.parse(s.startDate)?.time ?: 0L
        }.getOrDefault(0L)

        val startOdo = s.startOdometer.replace(',', '.').toDoubleOrNull() ?: 0.0
        val currentOdo = s.currentOdometer.replace(',', '.').toDoubleOrNull() ?: startOdo

        val initialContract = RentingContract(
            id = temporaryContractId,
            vehicleName = s.vehicleName,
            startDate = startTime,
            durationMonths = s.durationMonths.toIntOrNull() ?: 0,
            totalKms = s.totalKms.replace(',', '.').toDoubleOrNull() ?: 0.0,
            startOdometer = startOdo,
            currentOdometer = currentOdo,
            bluetoothDeviceAddress = s.bluetoothDeviceAddress,
            bluetoothDeviceName = s.bluetoothDeviceName,
            excessDistancePrice = s.excessDistancePrice.replace(',', '.').toDoubleOrNull(),
            courtesyMarginKms = s.courtesyMarginKms.replace(',', '.').toDoubleOrNull() ?: 0.0,
            fuelType = s.fuelType,
            syncStatus = SyncStatus.PENDING
        )

        val imageUri = s.selectedImageUri

        viewModelScope.launch {
            updateState { copy(isLoading = true) }

            val saveFlow = if (imageUri != null) {
                val bytesResult = getImageBytesUseCase(GetImageBytesUseCase.Input(imageUri.toString()))
                val bytes = (bytesResult.getOrNull() as? GetImageBytesUseCase.Output.Success)?.bytes

                if (bytes != null) {
                    val fileName = "vehicle_$temporaryContractId.jpg"
                    uploadVehicleImage(UploadVehicleImageUseCase.Input(bytes, fileName))
                        .flatMapLatest { output ->
                            when (output) {
                                is UploadVehicleImageUseCase.Output.Success -> {
                                    saveInitialContractUseCase(
                                        SaveInitialContractUseCase.Input(
                                            initialContract.copy(vehicleImageUrl = output.imageUrl)
                                        )
                                    )
                                }
                                is UploadVehicleImageUseCase.Output.Failure -> throw Exception("Upload failed")
                                else -> emptyFlow()
                            }
                        }
                } else {
                    saveInitialContractUseCase(SaveInitialContractUseCase.Input(initialContract))
                }
            } else {
                saveInitialContractUseCase(SaveInitialContractUseCase.Input(initialContract))
            }

            saveFlow
                .catch {
                    logger.e("SetupWizardVM", "Error saving contract", it)
                    updateState {
                        copy(
                            isLoading = false,
                            error = TextProvider.Resource(R.string.onboarding_register_error)
                        )
                    }
                }
                .onEach { output ->
                    when (output) {
                        is SaveInitialContractUseCase.Output.Success -> {
                            analytics.track(
                                AnalyticsEvent.Custom(
                                    "renting_setup_completed",
                                    mapOf(
                                        "has_bluetooth" to (s.bluetoothDeviceAddress != null),
                                        "has_advanced" to (s.excessDistancePrice.isNotBlank())
                                    )
                                )
                            )
                            launchEffect(Effect.NavigateToDashboard)
                        }
                        is SaveInitialContractUseCase.Output.Failure -> {
                            updateState {
                                copy(
                                    isLoading = false,
                                    error = output.error.toText()
                                )
                            }
                        }
                        else -> {}
                    }
                }.collect()
        }
    }
}
