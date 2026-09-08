package es.joshluq.kmsafe.feature.fleet.edit

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.GetImageBytesUseCase
import es.joshluq.kmsafe.domain.usecase.GetVehicleByIdUseCase
import es.joshluq.kmsafe.domain.usecase.UpdateContractUseCase
import es.joshluq.kmsafe.domain.usecase.UploadVehicleImageUseCase
import es.joshluq.kmsafe.feature.fleet.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import es.joshluq.kmsafe.core.ui.R as CoreR

@HiltViewModel(assistedFactory = EditContractViewModel.Factory::class)
class EditContractViewModel @AssistedInject constructor(
    @Assisted val vehicleId: String,
    private val getVehicleByIdUseCase: GetVehicleByIdUseCase,
    private val updateContractUseCase: UpdateContractUseCase,
    private val uploadVehicleImageUseCase: UploadVehicleImageUseCase,
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
    private val getImageBytesUseCase: GetImageBytesUseCase,
    private val analytics: AnalyticskitManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    @AssistedFactory
    interface Factory {
        fun create(vehicleId: String): EditContractViewModel
    }

    init {
        checkPremiumStatus()
        loadVehicle()
    }

    override fun createInitialState(): State = State.Empty

    override fun handleEvent(event: Event) {
        when (event) {
            Event.OnBackClicked -> launchEffect(Effect.NavigateBack)
            Event.OnSaveClicked -> handleSave()

            is Event.OnVehicleNameChanged -> {
                updateState { copy(vehicleName = event.value, vehicleNameError = null) }
                checkDirtyState()
            }
            is Event.OnFuelTypeChanged -> {
                updateState { copy(fuelType = event.value) }
                checkDirtyState()
            }
            is Event.OnOriginalImageSelected -> {
                event.uri?.let { launchEffect(Effect.NavigateToCropper(it.toString())) }
            }
            is Event.OnImageSelected -> {
                updateState { copy(selectedImageUri = event.uri) }
                checkDirtyState()
            }
            is Event.OnDurationMonthsChanged -> {
                updateState { copy(durationMonths = event.value, durationMonthsError = null) }
                checkDirtyState()
            }
            is Event.OnTotalKmsChanged -> {
                updateState { copy(totalKms = event.value, totalKmsError = null) }
                checkDirtyState()
            }
            is Event.OnBluetoothDeviceSelected -> {
                updateState {
                    copy(
                        bluetoothDeviceName = event.name,
                        bluetoothDeviceAddress = event.address,
                        showBluetoothPicker = false
                    )
                }
                checkDirtyState()
            }
            is Event.OnExcessDistancePriceChanged -> {
                updateState { copy(excessDistancePrice = event.value) }
                checkDirtyState()
            }
            is Event.OnCourtesyMarginKmsChanged -> {
                updateState { copy(courtesyMarginKms = event.value) }
                checkDirtyState()
            }

            Event.OnToggleBluetoothPicker -> {
                val newState = !state.value.showBluetoothPicker
                logger.d("EditContractVM", "Toggling bluetooth picker to: $newState")
                updateState { copy(showBluetoothPicker = newState) }
            }
            Event.OnDismissError -> updateState { copy(error = null) }
        }
    }

    private fun checkDirtyState() {
        val s = state.value
        val r = s.renting ?: return

        val isNameDirty = s.vehicleName != r.vehicleName
        val isFuelDirty = s.fuelType != r.fuelType
        val isImageDirty = s.selectedImageUri != null
        val isDurationDirty = s.durationMonths != r.durationMonths.toString()
        val isTotalKmsDirty = s.totalKms != r.totalKms.toString()
        val isBluetoothDirty = s.bluetoothDeviceAddress != r.bluetoothDeviceAddress
        val isPriceDirty = s.excessDistancePrice != (r.excessDistancePrice?.toString() ?: "")
        val isMarginDirty = s.courtesyMarginKms != r.courtesyMarginKms.toString()

        val dirty = isNameDirty || isFuelDirty || isImageDirty || isDurationDirty || isTotalKmsDirty || isBluetoothDirty || isPriceDirty || isMarginDirty
        updateState { copy(isDirty = dirty) }
    }

    private fun checkPremiumStatus() {
        checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.CLOUD_SYNC))
            .onEach { output ->
                if (output is CheckFeatureAccessUseCase.Output.Success) {
                    updateState { copy(isPremium = output.isGranted) }
                }
            }.launchIn(viewModelScope)
    }

    private fun loadVehicle() {
        getVehicleByIdUseCase(GetVehicleByIdUseCase.Input(vehicleId))
            .onEach { output ->
                when (output) {
                    is GetVehicleByIdUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                    is GetVehicleByIdUseCase.Output.Success -> {
                        val contract = output.contract
                        updateState {
                            copy(
                                isLoading = false,
                                renting = contract,
                                vehicleName = contract.vehicleName,
                                fuelType = contract.fuelType,
                                vehicleImageUrl = contract.vehicleImageUrl,
                                durationMonths = contract.durationMonths.toString(),
                                totalKms = contract.totalKms.toString(),
                                bluetoothDeviceAddress = contract.bluetoothDeviceAddress,
                                bluetoothDeviceName = contract.bluetoothDeviceName,
                                excessDistancePrice = contract.excessDistancePrice?.toString() ?: "",
                                courtesyMarginKms = contract.courtesyMarginKms.toString()
                            )
                        }
                    }
                    is GetVehicleByIdUseCase.Output.Failure -> updateState {
                        copy(isLoading = false, error = TextProvider.Resource(CoreR.string.history_load_error))
                    }
                }
            }.launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun handleSave() {
        if (!validate()) return

        val s = state.value
        val baseContract = s.renting ?: return

        val updatedContract = baseContract.copy(
            vehicleName = s.vehicleName,
            fuelType = s.fuelType,
            durationMonths = s.durationMonths.toIntOrNull() ?: baseContract.durationMonths,
            totalKms = s.totalKms.replace(',', '.').toDoubleOrNull() ?: baseContract.totalKms,
            bluetoothDeviceAddress = s.bluetoothDeviceAddress,
            bluetoothDeviceName = s.bluetoothDeviceName,
            excessDistancePrice = s.excessDistancePrice.replace(',', '.').toDoubleOrNull(),
            courtesyMarginKms = s.courtesyMarginKms.replace(',', '.').toDoubleOrNull() ?: 0.0
        )

        val imageUri = s.selectedImageUri

        viewModelScope.launch {
            updateState { copy(isSaving = true) }

            val flow = if (imageUri != null) {
                val bytesResult = getImageBytesUseCase(GetImageBytesUseCase.Input(imageUri.toString()))
                val bytes = (bytesResult.getOrNull() as? GetImageBytesUseCase.Output.Success)?.bytes

                if (bytes != null) {
                    val fileName = "vehicle_${baseContract.id}.jpg"
                    uploadVehicleImageUseCase(UploadVehicleImageUseCase.Input(bytes, fileName))
                        .flatMapLatest { output ->
                            when (output) {
                                is UploadVehicleImageUseCase.Output.Success -> {
                                    updateContractUseCase(
                                        UpdateContractUseCase.Input(
                                            updatedContract.copy(vehicleImageUrl = output.imageUrl)
                                        )
                                    )
                                }
                                is UploadVehicleImageUseCase.Output.Failure -> throw Exception("Image upload failed")
                                else -> emptyFlow()
                            }
                        }
                } else {
                    updateContractUseCase(UpdateContractUseCase.Input(updatedContract))
                }
            } else {
                updateContractUseCase(UpdateContractUseCase.Input(updatedContract))
            }

            flow
                .catch {
                    logger.e("EditContractVM", "Error updating contract", it)
                    updateState {
                        copy(
                            isSaving = false,
                            error = TextProvider.Resource(R.string.onboarding_register_error)
                        )
                    }
                }
                .onEach { output ->
                    when (output) {
                        is UpdateContractUseCase.Output.Success -> {
                            analytics.track(AnalyticsEvent.Custom("vehicle_updated", mapOf("id" to vehicleId)))
                            launchEffect(Effect.NavigateBack)
                        }
                        is UpdateContractUseCase.Output.Failure -> {
                            updateState {
                                copy(
                                    isSaving = false,
                                    error = TextProvider.Resource(R.string.onboarding_register_error)
                                )
                            }
                        }
                        else -> {}
                    }
                }.collect()
        }
    }

    private fun validate(): Boolean {
        val s = state.value
        var isValid = true
        if (s.vehicleName.isBlank()) {
            updateState { copy(vehicleNameError = TextProvider.Resource(R.string.onboarding_vehicle_name_feedback)) }
            isValid = false
        }
        val duration = s.durationMonths.toIntOrNull()
        if (duration == null || duration <= 0) {
            updateState { copy(durationMonthsError = TextProvider.Resource(R.string.onboarding_number_feedback)) }
            isValid = false
        }
        val kms = s.totalKms.replace(',', '.').toDoubleOrNull()
        if (kms == null || kms <= 0) {
            updateState { copy(totalKmsError = TextProvider.Resource(R.string.onboarding_number_feedback)) }
            isValid = false
        }

        val price = s.excessDistancePrice.replace(',', '.').toDoubleOrNull()
        if (s.excessDistancePrice.isNotBlank() && (price == null || price < 0)) {
            // We could add excessDistancePriceError to the state if needed, for now using a generic banner error or just invalidating
            isValid = false
        }

        val margin = s.courtesyMarginKms.replace(',', '.').toDoubleOrNull()
        if (s.courtesyMarginKms.isNotBlank() && (margin == null || margin < 0)) {
            isValid = false
        }

        return isValid
    }
}
