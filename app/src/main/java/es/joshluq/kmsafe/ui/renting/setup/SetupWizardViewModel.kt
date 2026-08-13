package es.joshluq.kmsafe.ui.renting.setup

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.di.GetEntitlements
import es.joshluq.kmsafe.di.GetImageBytes
import es.joshluq.kmsafe.di.SaveInitialContract
import es.joshluq.kmsafe.di.UploadVehicleImage
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.SyncStatus
import es.joshluq.kmsafe.domain.usecase.GetEntitlementsUseCase
import es.joshluq.kmsafe.domain.usecase.GetImageBytesUseCase
import es.joshluq.kmsafe.domain.usecase.SaveInitialContractUseCase
import es.joshluq.kmsafe.domain.usecase.UploadVehicleImageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SetupWizardViewModel @Inject constructor(
    @param:SaveInitialContract private val saveInitialContractUseCase:
    @JvmSuppressWildcards FlowUseCase<SaveInitialContractUseCase.Input, SaveInitialContractUseCase.Output>,
    @param:GetEntitlements private val getEntitlementsUseCase:
    @JvmSuppressWildcards FlowUseCase<GetEntitlementsUseCase.Input, GetEntitlementsUseCase.Output>,
    @param:UploadVehicleImage private val uploadVehicleImage:
    @JvmSuppressWildcards FlowUseCase<UploadVehicleImageUseCase.Input, UploadVehicleImageUseCase.Output>,
    @param:GetImageBytes private val getImageBytesUseCase:
    @JvmSuppressWildcards UseCase<GetImageBytesUseCase.Input, GetImageBytesUseCase.Output>,
    private val analytics: AnalyticskitManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    private val temporaryContractId = UUID.randomUUID().toString()

    init {
        loadEntitlements()
    }

    override fun createInitialState(): State = State.Empty

    override fun handleEvent(event: Event) {
        when (event) {
            Event.OnNextClicked -> handleNext()
            Event.OnBackClicked -> handleBack()
            Event.OnSkipStepClicked -> handleSkip()

            is Event.OnVehicleNameChanged -> updateState { copy(vehicleName = event.value, vehicleNameError = null) }
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
            is Event.OnTotalKmsChanged -> updateState { copy(totalKms = event.value, totalKmsError = null) }
            is Event.OnStartOdometerChanged -> updateState {
                copy(
                    startOdometer = event.value,
                    startOdometerError = null
                )
            }
            is Event.OnCurrentOdometerChanged -> updateState { copy(currentOdometer = event.value) }
            is Event.OnBluetoothDeviceSelected -> updateState {
                copy(
                    bluetoothDeviceName = event.name,
                    bluetoothDeviceAddress = event.address,
                    showBluetoothPicker = false
                )
            }
            is Event.OnExcessDistancePriceChanged -> updateState { copy(excessDistancePrice = event.value) }
            is Event.OnCourtesyMarginKmsChanged -> updateState { copy(courtesyMarginKms = event.value) }

            Event.OnToggleDatePicker -> updateState { copy(showDatePicker = !showDatePicker) }
            Event.OnToggleBluetoothPicker -> updateState { copy(showBluetoothPicker = !showBluetoothPicker) }
            Event.OnDismissError -> updateState { copy(error = null) }
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
                updateState { copy(currentStep = SetupStep.ADVANCED_PROTECTION) }
            }
            SetupStep.ADVANCED_PROTECTION -> {
                saveContract()
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
        var isValid = true
        if (state.value.startDate.isBlank()) {
            updateState { copy(startDateError = TextProvider.Resource(R.string.onboarding_date_feedback)) }
            isValid = false
        }
        if (state.value.durationMonths.toIntOrNull() == null) {
            updateState { copy(durationMonthsError = TextProvider.Resource(R.string.onboarding_number_feedback)) }
            isValid = false
        }
        return isValid
    }

    private fun validateMileage(): Boolean {
        var isValid = true
        if (state.value.totalKms.toIntOrNull() == null) {
            updateState { copy(totalKmsError = TextProvider.Resource(R.string.onboarding_number_feedback)) }
            isValid = false
        }
        if (state.value.startOdometer.toIntOrNull() == null) {
            updateState { copy(startOdometerError = TextProvider.Resource(R.string.onboarding_number_feedback)) }
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun saveContract() {
        val s = state.value
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val startTime = runCatching { sdf.parse(s.startDate)?.time ?: 0L }.getOrDefault(0L)

        val initialContract = RentingContract(
            id = temporaryContractId,
            vehicleName = s.vehicleName,
            startDate = startTime,
            durationMonths = s.durationMonths.toIntOrNull() ?: 0,
            totalKms = s.totalKms.toIntOrNull() ?: 0,
            startOdometer = s.startOdometer.toIntOrNull() ?: 0,
            currentOdometer = s.currentOdometer.toIntOrNull() ?: s.startOdometer.toIntOrNull() ?: 0,
            bluetoothDeviceAddress = s.bluetoothDeviceAddress,
            bluetoothDeviceName = s.bluetoothDeviceName,
            excessDistancePrice = s.excessDistancePrice.toDoubleOrNull(),
            courtesyMarginKms = s.courtesyMarginKms.toIntOrNull() ?: 0,
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
                                    error = TextProvider.Resource(R.string.onboarding_register_error)
                                )
                            }
                        }
                        else -> {}
                    }
                }.collect()
        }
    }
}
