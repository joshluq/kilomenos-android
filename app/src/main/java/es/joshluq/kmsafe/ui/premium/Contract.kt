package es.joshluq.kmsafe.ui.premium

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState

/**
 * Represents the UI state for the Premium Paywall screen.
 */
data class State(
    val isLoading: Boolean = false,
    val isMigrating: Boolean = false,
    val error: TextProvider? = null
) : UiState {
    companion object {
        val Empty = State()
    }
}

/**
 * Represents the UI events for the Premium Paywall screen.
 */
sealed interface Event : UiEvent {
    object OnUpgradeClicked : Event
    object OnDismissClicked : Event
    object OnDismissError : Event
}

/**
 * Represents the side effects for the Premium Paywall screen.
 */
sealed interface Effect : UiEffect {
    object LaunchBillingFlow : Effect
    object NavigateToDashboard : Effect
    object NavigateBack : Effect
}
