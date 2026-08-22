package es.joshluq.kmsafe.ui.dashboard

import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState

/**
 * Represents the UI state for the Dashboard screen.
 */
data class State(
    val selectedTab: DashboardTab = DashboardTab.OVERVIEW,
    val showUpdateDialog: Boolean = false,
    val currentMileageInput: String = "",
    val hasRentingContract: Boolean = false
) : UiState {
    companion object {
        val Empty = State()
    }
}

/**
 * Represents the UI events that can be triggered from the Dashboard screen.
 */
sealed interface Event : UiEvent {
    data class OnTabSelected(val tab: DashboardTab) : Event
    data class OnTabSynced(val tab: DashboardTab) : Event
    data object OnUpdateOdometerClicked : Event
    data object OnDismissOdometerDialog : Event
    data class OnMileageInputChanged(val mileage: String) : Event
}

/**
 * Represents the side effects that can occur on the Dashboard screen.
 */
sealed interface Effect : UiEffect {
    data class NavigateToTab(val tab: DashboardTab) : Effect
}

/**
 * Enum representing the available tabs in the Bottom Navigation.
 */
enum class DashboardTab {
    OVERVIEW, HISTORY, EXPENSES, PROJECTION, PROFILE
}
