package es.joshluq.kmsafe.feature.auth.login

import androidx.compose.runtime.Immutable
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.User

/**
 * Represents the UI state for the Login screen.
 */
@Immutable
data class State(
    val email: String = "",
    val emailError: TextProvider? = null,
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoginEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val showUserConflictWarning: Boolean = false,
    val pendingUser: User? = null,
    val termsUrl: String = "",
    val privacyUrl: String = "",
    val error: TextProvider? = null
) : UiState {

    companion object {
        val Empty = State()
    }
}

/**
 * Represents the UI events that can be triggered from the Login screen.
 */
sealed interface Event : UiEvent {
    data class OnEmailChanged(val value: String) : Event
    data class OnPasswordChanged(val value: String) : Event
    data object OnTogglePasswordVisibility : Event
    data object OnLoginClicked : Event
    data object OnConfirmUserConflict : Event
    data object OnDismissUserConflict : Event
    data object OnGoogleSignInClicked : Event
    data object OnDismissError : Event
}

/**
 * Represents the side effects that can occur on the Login screen.
 */
sealed interface Effect : UiEffect {
    data object NavigateToDashboard : Effect
    data object NavigateToPremiumPaywall : Effect
    data object TriggerGoogleSignIn : Effect
}
