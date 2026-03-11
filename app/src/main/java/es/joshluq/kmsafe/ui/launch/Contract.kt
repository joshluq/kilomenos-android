package es.joshluq.kmsafe.ui.launch

import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState

/**
 * Represents the UI state for the Launch screen.
 *
 * @property isLoading Whether the session validation is in progress.
 */
data class LaunchState(val isLoading: Boolean = true) : UiState

/**
 * Represents the UI events that can be triggered from the Launch screen.
 */
sealed interface LaunchEvent : UiEvent

/**
 * Represents the side effects that can occur on the Launch screen.
 */
sealed interface LaunchEffect : UiEffect {
    /**
     * Effect to navigate to the Login screen.
     */
    data object NavigateToLogin : LaunchEffect

    /**
     * Effect to navigate to the Dashboard.
     */
    data object NavigateToDashboard : LaunchEffect
}
