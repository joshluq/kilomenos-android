package es.joshluq.kmsafe.feature.fleet.edit

import android.net.Uri
import androidx.compose.runtime.Immutable
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.RentingContract

@Immutable
data class State(
    val renting: RentingContract? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isPremium: Boolean = false,

    // Editable fields
    val vehicleName: String = "",
    val fuelType: FuelType = FuelType.GASOLINE_95,
    val vehicleImageUrl: String? = null,
    val selectedImageUri: Uri? = null,
    val durationMonths: String = "",
    val totalKms: String = "",
    val bluetoothDeviceName: String? = null,
    val bluetoothDeviceAddress: String? = null,
    val excessDistancePrice: String = "",
    val courtesyMarginKms: String = "",

    // UI State
    val showBluetoothPicker: Boolean = false,
    val error: TextProvider? = null,
    val vehicleNameError: TextProvider? = null,
    val durationMonthsError: TextProvider? = null,
    val totalKmsError: TextProvider? = null,
    val isDirty: Boolean = false
) : UiState {
    companion object {
        val Empty = State()
    }
}

sealed interface Event : UiEvent {
    data object OnBackClicked : Event
    data object OnSaveClicked : Event

    // Input Changes
    data class OnVehicleNameChanged(val value: String) : Event
    data class OnFuelTypeChanged(val value: FuelType) : Event
    data class OnOriginalImageSelected(val uri: Uri?) : Event
    data class OnImageSelected(val uri: Uri) : Event
    data class OnDurationMonthsChanged(val value: String) : Event
    data class OnTotalKmsChanged(val value: String) : Event
    data class OnBluetoothDeviceSelected(val name: String, val address: String) : Event
    data class OnExcessDistancePriceChanged(val value: String) : Event
    data class OnCourtesyMarginKmsChanged(val value: String) : Event

    // Dialogs/Pickers
    data object OnToggleBluetoothPicker : Event
    data object OnDismissError : Event
}

sealed interface Effect : UiEffect {
    data object NavigateBack : Effect
    data class NavigateToCropper(val uri: String) : Effect
}
