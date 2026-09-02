package es.joshluq.kmsafe.feature.profile

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.model.User

data class State(
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val deletionMessage: TextProvider? = null,
    val showLogoutConfirmation: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val user: User? = null,
    val entitlements: Entitlements? = null,
    val termsUrl: String = "",
    val privacyUrl: String = "",
    val error: TextProvider? = null
) : UiState

sealed interface Event : UiEvent {
    object OnVehiclesClicked : Event
    object OnDataManagementClicked : Event
    object OnPreferencesClicked : Event
    object OnLogoutClicked : Event
    object OnLogoutConfirmed : Event
    object OnLogoutCancelled : Event
    object OnUpgradeClicked : Event
    object OnDeleteAccountClicked : Event
    object OnDeleteAccountConfirmed : Event
    object OnDeleteAccountCancelled : Event
    object OnWelcomeGuideClicked : Event
    object OnDismissError : Event
}

sealed interface Effect : UiEffect {
    object NavigateToVehicles : Effect
    object NavigateToDataManagement : Effect
    object NavigateToPreferences : Effect
    object NavigateToLogin : Effect
    object NavigateToPremiumPaywall : Effect
    object NavigateToWelcomeDiscovery : Effect
    data class ShowMessage(val message: String) : Effect
}
