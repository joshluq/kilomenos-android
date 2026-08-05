package es.joshluq.kmsafe.ui.profile.preferences

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState

data class State(
    val rememberEmail: Boolean = true,
    val showProjectionBanner: Boolean = true,
    val autoTrackingEnabled: Boolean = false,
    val isUserPremium: Boolean = false,
    val isPrivacyOptionsRequired: Boolean = false,
    val isLoading: Boolean = false,
    val error: TextProvider? = null
) : UiState {
    companion object {
        val Empty = State()
    }
}

sealed interface Event : UiEvent {
    data class OnRememberEmailToggled(val enabled: Boolean) : Event
    data class OnProjectionBannerToggled(val enabled: Boolean) : Event
    data class OnAutoTrackingToggled(val enabled: Boolean) : Event
    data object OnManagePrivacyClicked : Event
    data object OnBackClicked : Event
    data object OnDismissError : Event
    data object OnPermissionsRationaleSuccess : Event
}

sealed interface Effect : UiEffect {
    data object ShowPrivacyOptions : Effect
    data object NavigateBack : Effect
    data object NavigateToPermissions : Effect
}
