package es.joshluq.kmsafe.ui.renting.detail

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.RentingContract

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

sealed interface Event : UiEvent {
    data object OnBackClicked : Event
    data object OnEditClicked : Event
    data object OnDeleteClicked : Event
    data object OnConfirmDelete : Event
    data object OnCancelDelete : Event
    data object OnDismissError : Event
}

sealed interface Effect : UiEffect {
    data object NavigateBack : Effect
    data class NavigateToEdit(val vehicleId: String) : Effect
}
