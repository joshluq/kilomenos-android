package es.joshluq.kmsafe.ui.onboarding

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.di.GetVehicleById
import es.joshluq.kmsafe.di.SaveInitialContract
import es.joshluq.kmsafe.di.UpdateContract
import es.joshluq.kmsafe.di.UploadVehicleImage
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.usecase.GetVehicleByIdUseCase
import es.joshluq.kmsafe.domain.usecase.SaveInitialContractUseCase
import es.joshluq.kmsafe.domain.usecase.UpdateContractUseCase
import es.joshluq.kmsafe.domain.usecase.UploadVehicleImageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the Onboarding screen, responsible for managing the initial vehicle setup.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    @param:SaveInitialContract private val saveInitialContract:
    @JvmSuppressWildcards FlowUseCase<SaveInitialContractUseCase.Input, SaveInitialContractUseCase.Output>,
    @param:GetVehicleById private val getVehicleById:
    @JvmSuppressWildcards FlowUseCase<GetVehicleByIdUseCase.Input, GetVehicleByIdUseCase.Output>,
    @param:UpdateContract private val updateContract:
    @JvmSuppressWildcards FlowUseCase<UpdateContractUseCase.Input, UpdateContractUseCase.Output>,
    @param:UploadVehicleImage private val uploadVehicleImage:
    @JvmSuppressWildcards FlowUseCase<UploadVehicleImageUseCase.Input, UploadVehicleImageUseCase.Output>,
    private val analytics: AnalyticskitManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    private val vehicleId: String? = savedStateHandle.get<String>("vehicleId")
    private val isEditParam: Boolean = savedStateHandle.get<Boolean>("isEdit") ?: false

    init {
        loadData()
        if (vehicleId == null) {
            analytics.track(AnalyticsEvent.FunnelStep("onboarding", "started"))
        }
    }

    override fun createInitialState(): State = State.Empty

    override fun handleEvent(event: Event) {
        logger.d("OnboardingViewModel", "Event received: $event")
        when (event) {
            is Event.OnStartOdometerChanged -> handleOnStartOdometerChanged(event.startOdometer)
            is Event.OnCurrentOdometerChanged -> handleOnCurrentOdometerChanged(event.currentOdometer)
            is Event.OnDurationMonthsChanged -> handleOnDurationMonthsChanged(event.durationMonths)
            is Event.OnStartDateChanged -> handleOnStartDateChanged(event.startDate)
            is Event.OnTotalKmsChanged -> handleOnTotalKmsChanged(event.totalKms)
            is Event.OnVehicleNameChanged -> handleOnVehicleNameChanged(event.vehicleName)
            is Event.OnOriginalImageSelected -> handleOnOriginalImageSelected(event.uri)
            is Event.OnImageSelected -> updateState { copy(selectedImageUri = event.uri) }
            Event.OnEditModeRequested -> updateState { copy(isReadOnly = false, isEditMode = true) }
            Event.OnDismissError -> updateState { copy(error = null) }
            is Event.OnToggleDatePicker -> handleOnToggleDatePicker()
            is Event.OnRegisterClicked -> handleOnRegisterClicked()
        }
    }

    /**
     * Loads existing contract data if available to pre-fill the form in read-only mode.
     */
    private fun loadData() {
        if (vehicleId == null) {
            updateState { copy(isLoading = false, isReadOnly = false) }
            return
        }

        getVehicleById(GetVehicleByIdUseCase.Input(vehicleId))
            .onEach { output ->
                when (output) {
                    is GetVehicleByIdUseCase.Output.Success -> {
                        val contract = output.contract
                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        updateState {
                            copy(
                                isLoading = false,
                                vehicleName = contract.vehicleName,
                                startDate = formatter.format(Date(contract.startDate)),
                                durationMonths = contract.durationMonths.toString(),
                                totalKms = contract.totalKms.toString(),
                                startOdometer = contract.startOdometer.toString(),
                                currentOdometer = contract.currentOdometer.toString(),
                                vehicleImageUrl = contract.vehicleImageUrl,
                                renting = contract,
                                isReadOnly = !isEditParam,
                                isEditMode = isEditParam
                            )
                        }
                    }

                    is GetVehicleByIdUseCase.Output.Failure -> {
                        updateState { 
                            copy(
                                isLoading = false,
                                error = TextProvider.Resource(R.string.history_load_error)
                            ) 
                        }
                    }

                    else -> { }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleOnVehicleNameChanged(vehicleName: String) {
        if (state.value.isReadOnly) return
        updateState { copy(vehicleName = vehicleName, vehicleNameError = null) }
    }

    private fun handleOnTotalKmsChanged(totalKms: String) {
        if (state.value.isReadOnly) return
        updateState { copy(totalKms = totalKms, totalKmsError = null) }
    }

    private fun handleOnStartDateChanged(startDate: String) {
        if (state.value.isReadOnly) return
        updateState { copy(startDate = startDate, startDateError = null) }
    }

    private fun handleOnDurationMonthsChanged(durationMonths: String) {
        if (state.value.isReadOnly) return
        updateState { copy(durationMonths = durationMonths, durationMonthsError = null) }
    }

    private fun handleOnStartOdometerChanged(startOdometer: String) {
        if (state.value.isReadOnly) return
        updateState { copy(startOdometer = startOdometer, startOdometerError = null) }
    }

    private fun handleOnCurrentOdometerChanged(currentOdometer: String) {
        if (state.value.isReadOnly) return
        updateState { copy(currentOdometer = currentOdometer, currentOdometerError = null) }
    }

    private fun handleOnOriginalImageSelected(uri: android.net.Uri?) {
        uri?.let {
            launchEffect(Effect.NavigateToCropper(it.toString()))
        }
    }

    private fun handleOnToggleDatePicker() {
        if (state.value.isReadOnly) return
        updateState { copy(showDatePicker = !showDatePicker) }
    }

    /**
     * Handles the register button click, triggers validation and persistence if valid.
     */
    private fun handleOnRegisterClicked() {
        if (state.value.isReadOnly) return

        validateAndGetContract().onSuccess { contract ->
            saveContract(contract)
        }
    }

    /**
     * Validates the form data and returns a [RentingContract] if valid.
     * Updates the UI state with errors if validation fails.
     */
    private fun validateAndGetContract(): Result<RentingContract> {
        val currentState = state.value
        val vehicleNameValid = currentState.vehicleName.isNotBlank()
        val duration = currentState.durationMonths.toIntOrNull() ?: 0
        val totalKms = currentState.totalKms.toIntOrNull() ?: 0
        val startOdometer = currentState.startOdometer.toIntOrNull() ?: -1
        val currentOdometer = currentState.currentOdometer.toIntOrNull() ?: -1

        val dateResult = runCatching {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(currentState.startDate)?.time
                ?: throw IllegalArgumentException("Invalid date")
        }

        val dateMillis = dateResult.getOrDefault(0L)
        val durationValid = duration > 0
        val totalKmsValid = totalKms > 0
        val startOdometerValid = startOdometer >= 0
        val currentOdometerValid = currentOdometer >= startOdometer && currentOdometer >= 0
        val startDateValid = dateResult.isSuccess

        if (!vehicleNameValid || !durationValid || !totalKmsValid || !startOdometerValid || !currentOdometerValid || !startDateValid) {
            updateValidationErrors(
                vehicleNameValid,
                durationValid,
                totalKmsValid,
                startOdometerValid,
                currentOdometerValid,
                startDateValid
            )
            return Result.failure(IllegalStateException("Validation failed"))
        }

        return Result.success(
            RentingContract(
                id = currentState.renting?.id ?: "",
                userId = currentState.renting?.userId ?: "",
                vehicleName = currentState.vehicleName,
                startDate = dateMillis,
                durationMonths = duration,
                totalKms = totalKms,
                startOdometer = startOdometer,
                currentOdometer = currentOdometer,
                isSelected = currentState.renting?.isSelected ?: true,
                vehicleImageUrl = currentState.vehicleImageUrl
            )
        )
    }

    /**
     * Updates the UI state with validation error messages.
     */
    private fun updateValidationErrors(
        vehicleNameValid: Boolean,
        durationValid: Boolean,
        totalKmsValid: Boolean,
        startOdometerValid: Boolean,
        currentOdometerValid: Boolean,
        startDateValid: Boolean
    ) {
        updateState {
            copy(
                vehicleNameError = if (vehicleNameValid) {
                    null
                } else {
                    TextProvider.Resource(
                        R.string.onboarding_vehicle_name_feedback
                    )
                },
                durationMonthsError = if (durationValid) {
                    null
                } else {
                    TextProvider.Resource(
                        R.string.onboarding_number_feedback
                    )
                },
                totalKmsError = if (totalKmsValid) null else TextProvider.Resource(R.string.onboarding_number_feedback),
                startOdometerError = if (startOdometerValid) {
                    null
                } else {
                    TextProvider.Resource(
                        R.string.onboarding_number_feedback
                    )
                },
                currentOdometerError = if (currentOdometerValid) {
                    null
                } else {
                    TextProvider.Resource(
                        R.string.onboarding_current_odometer_feedback
                    )
                },
                startDateError = if (startDateValid) null else TextProvider.Resource(R.string.onboarding_date_feedback)
            )
        }
    }

    /**
     * Executes the use case to save or update the contract.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun saveContract(contract: RentingContract) {
        val imageUri = state.value.selectedImageUri
        val isEdit = state.value.isEditMode

        val flow = if (imageUri != null) {
            val bytes = readBytes(imageUri)
            if (bytes != null) {
                val fileName = "vehicle_${UUID.randomUUID()}.jpg"
                uploadVehicleImage(UploadVehicleImageUseCase.Input(bytes, fileName))
                    .flatMapLatest { output ->
                        when (output) {
                            is UploadVehicleImageUseCase.Output.Success -> {
                                executeSaveAction(contract.copy(vehicleImageUrl = output.imageUrl), isEdit)
                            }
                            is UploadVehicleImageUseCase.Output.Failure -> {
                                throw Exception("Image upload failed")
                            }
                            else -> emptyFlow()
                        }
                    }
            } else {
                executeSaveAction(contract, isEdit)
            }
        } else {
            executeSaveAction(contract, isEdit)
        }

        flow
            .catch {
                logger.e("OnboardingViewModel", "Error in save flow", it)
                updateState {
                    copy(
                        isLoading = false,
                        error = TextProvider.Resource(R.string.onboarding_register_error)
                    )
                }
            }
            .onEach { output ->
                when (output) {
                    is SaveInitialContractUseCase.Output.Progress, is UpdateContractUseCase.Output.Progress, is UploadVehicleImageUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                    is SaveInitialContractUseCase.Output.Success, is UpdateContractUseCase.Output.Success -> handleSuccess()
                    is SaveInitialContractUseCase.Output.Failure, is UpdateContractUseCase.Output.Failure, is UploadVehicleImageUseCase.Output.Failure -> handleFailure()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun executeSaveAction(contract: RentingContract, isEdit: Boolean): Flow<Any> {
        return if (isEdit) {
            updateContract(UpdateContractUseCase.Input(contract))
        } else {
            saveInitialContract(SaveInitialContractUseCase.Input(contract))
        }
    }

    private fun handleSuccess() {
        updateState { copy(isLoading = false) }
        analytics.track(AnalyticsEvent.FunnelStep("onboarding", "completed"))
        launchEffect(Effect.NavigateBack)
    }

    private fun handleFailure() {
        updateState {
            copy(
                isLoading = false,
                error = TextProvider.Resource(R.string.onboarding_register_error)
            )
        }
    }

    private fun readBytes(uri: android.net.Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            logger.e("OnboardingViewModel", "Error reading image bytes", e)
            null
        }
    }
}
