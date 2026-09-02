package es.joshluq.kmsafe.feature.fleet.detail

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.RentingContract

/**
 * Represents the UI state for the Vehicle Detail screen.
 */
data class State(
    val renting: RentingContract? = null,
    val isLoading: Boolean = true,
    val isPremium: Boolean = false,
    val error: TextProvider? = null
) : UiState {
    companion object {
        val Empty = State()
    }
}

/**
 * Represents the UI events that can be triggered from the Vehicle Detail screen.
 */
sealed interface Event : UiEvent {
    data object OnBackClicked : Event
    data object OnEditClicked : Event
    data object OnDeleteClicked : Event
    data object OnConfirmDelete : Event
    data object OnCancelDelete : Event
    data object OnDismissError : Event
}

/**
 * Represents the side effects that can occur on the Vehicle Detail screen.
 */
sealed interface Effect : UiEffect {
    data object NavigateBack : Effect
    data class NavigateToEdit(val vehicleId: String) : Effect
}
