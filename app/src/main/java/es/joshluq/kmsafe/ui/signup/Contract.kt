package es.joshluq.kmsafe.ui.signup

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState

/**
 * Represents the UI state for the Signup screen.
 */
data class State(
    val name: String = "",
    val email: String = "",
    val emailError: TextProvider? = null,
    val password: String = "",
    val passwordError: TextProvider? = null,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isSignupEnabled: Boolean = false,
    val showUserConflictWarning: Boolean = false,
    val error: TextProvider? = null
) : UiState {
    companion object {
        val Empty = State()
    }
}

/**
 * Represents the UI events for the Signup screen.
 */
sealed interface Event : UiEvent {
    data class OnNameChanged(val value: String) : Event
    data class OnEmailChanged(val value: String) : Event
    data class OnPasswordChanged(val value: String) : Event
    object OnTogglePasswordVisibility : Event
    object OnSignupClicked : Event
    object OnConfirmUserConflict : Event
    object OnDismissUserConflict : Event
    object OnDismissError : Event
    object OnBackClicked : Event
}

/**
 * Represents the side effects for the Signup screen.
 */
sealed interface Effect : UiEffect {
    object NavigateBack : Effect
    object NavigateToDashboard : Effect
    object NavigateToPremiumPaywall : Effect
}
