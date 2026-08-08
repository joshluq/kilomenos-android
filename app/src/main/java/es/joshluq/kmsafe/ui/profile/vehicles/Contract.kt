package es.joshluq.kmsafe.ui.profile.vehicles

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.RentingContract

/**
 * Represents the UI state for the Vehicle List screen.
 */
data class State(
    val isLoading: Boolean = false,
    val vehicles: List<RentingContract> = emptyList(),
    val vehicleToDelete: RentingContract? = null,
    val showPremiumLimit: Boolean = false,
    val error: TextProvider? = null
) : UiState

/**
 * Represents the UI events that can be triggered from the Vehicle List screen.
 */
sealed interface Event : UiEvent {
    data class OnVehicleSelected(val id: String) : Event
    data class OnVehicleDetailsClicked(val id: String) : Event
    data class OnDeleteVehicleClicked(val vehicle: RentingContract) : Event
    object OnDeleteConfirmed : Event
    object OnDeleteCancelled : Event
    object OnAddVehicleClicked : Event
    object OnBackClicked : Event
    object OnUpgradeClicked : Event
    object OnDismissPremiumLimit : Event
    object OnDismissError : Event
}

/**
 * Represents the side effects that can occur on the Vehicle List screen.
 */
sealed interface Effect : UiEffect {
    object NavigateBack : Effect
    object NavigateToAddVehicle : Effect
    object NavigateToPremiumPaywall : Effect
    data class NavigateToVehicleDetails(val id: String) : Effect
}
