package es.joshluq.kmsafe.ui.onboarding

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.RentingContract

/**
 * Represents the UI state for the Onboarding screen.
 */
data class State(
    val vehicleName: String = "",
    val vehicleNameError: TextProvider? = null,
    val startDate: String = "",
    val startDateError: TextProvider? = null,
    val durationMonths: String = "",
    val durationMonthsError: TextProvider? = null,
    val totalKms: String = "",
    val totalKmsError: TextProvider? = null,
    val startOdometer: String = "",
    val startOdometerError: TextProvider? = null,
    val currentOdometer: String = "",
    val currentOdometerError: TextProvider? = null,
    val vehicleImageUrl: String? = null,
    val selectedImageUri: android.net.Uri? = null,
    val isUploadingImage: Boolean = false,
    val showDatePicker: Boolean = false,
    val isLoading: Boolean = true,
    val isReadOnly: Boolean = false,
    val isEditMode: Boolean = false,
    val renting: RentingContract? = null,
    val error: TextProvider? = null
) : UiState {

    companion object {
        val Empty = State()
    }
}

/**
 * Represents the UI events that can be triggered from the Onboarding screen.
 */
sealed interface Event : UiEvent {
    data class OnVehicleNameChanged(val vehicleName: String) : Event
    data class OnStartDateChanged(val startDate: String) : Event
    data class OnDurationMonthsChanged(val durationMonths: String) : Event
    data class OnTotalKmsChanged(val totalKms: String) : Event
    data class OnStartOdometerChanged(val startOdometer: String) : Event
    data class OnCurrentOdometerChanged(val currentOdometer: String) : Event
    data class OnOriginalImageSelected(val uri: android.net.Uri?) : Event
    data class OnImageSelected(val uri: android.net.Uri?) : Event
    data object OnEditModeRequested : Event
    data object OnToggleDatePicker : Event
    data object OnRegisterClicked : Event
    data object OnDismissError : Event
}

/**
 * Represents the side effects that can occur on the Onboarding screen.
 */
sealed interface Effect : UiEffect {
    data object NavigateBack : Effect
    data class NavigateToCropper(val uri: String) : Effect
}
