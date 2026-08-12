package es.joshluq.kmsafe.ui.renting.setup

import android.net.Uri
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.SubscriptionLevel

data class State(
    val currentStep: SetupStep = SetupStep.VEHICLE_IDENTITY,
    val isLoading: Boolean = false,
    val subscriptionLevel: SubscriptionLevel = SubscriptionLevel.FREE,

    // Step 1: Identity
    val vehicleName: String = "",
    val vehicleImageUrl: String? = null,
    val selectedImageUri: Uri? = null,

    // Step 2: Timeframe
    val startDate: String = "",
    val durationMonths: String = "",

    // Step 3: Mileage
    val totalKms: String = "",
    val startOdometer: String = "",
    val currentOdometer: String = "",

    // Step 4: Bluetooth
    val bluetoothDeviceName: String? = null,
    val bluetoothDeviceAddress: String? = null,

    // Step 5: Advanced
    val excessDistancePrice: String = "",
    val courtesyMarginKms: String = "",

    // UI State
    val showDatePicker: Boolean = false,
    val showBluetoothPicker: Boolean = false,
    val error: TextProvider? = null,
    val vehicleNameError: TextProvider? = null,
    val startDateError: TextProvider? = null,
    val durationMonthsError: TextProvider? = null,
    val totalKmsError: TextProvider? = null,
    val startOdometerError: TextProvider? = null
) : UiState {
    companion object {
        val Empty = State()
    }
}

enum class SetupStep(val index: Int) {
    VEHICLE_IDENTITY(1),
    CONTRACT_TIMEFRAME(2),
    MILEAGE_BUDGET(3),
    SMART_ACTIVATION(4),
    ADVANCED_PROTECTION(5);

    companion object {
        val totalSteps = entries.size
    }
}

sealed interface Event : UiEvent {
    // Navigation
    data object OnNextClicked : Event
    data object OnBackClicked : Event
    data object OnSkipStepClicked : Event

    // Input Changes
    data class OnVehicleNameChanged(val value: String) : Event
    data class OnOriginalImageSelected(val uri: Uri?) : Event
    data class OnImageSelected(val uri: Uri) : Event
    data class OnStartDateChanged(val value: String) : Event
    data class OnDurationMonthsChanged(val value: String) : Event
    data class OnTotalKmsChanged(val value: String) : Event
    data class OnStartOdometerChanged(val value: String) : Event
    data class OnCurrentOdometerChanged(val value: String) : Event
    data class OnBluetoothDeviceSelected(val name: String, val address: String) : Event
    data class OnExcessDistancePriceChanged(val value: String) : Event
    data class OnCourtesyMarginKmsChanged(val value: String) : Event

    // Dialogs/Pickers
    data object OnToggleDatePicker : Event
    data object OnToggleBluetoothPicker : Event
    data object OnDismissError : Event
}

sealed interface Effect : UiEffect {
    data object NavigateBack : Effect
    data object NavigateToDashboard : Effect
    data class NavigateToCropper(val uri: String) : Effect
}
