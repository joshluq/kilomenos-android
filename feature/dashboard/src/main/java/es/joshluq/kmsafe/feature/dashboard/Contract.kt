package es.joshluq.kmsafe.feature.dashboard

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.AppOverlayState

/**
 * Represents the UI state for the Dashboard screen.
 */
data class State(
    val selectedTab: DashboardTab = DashboardTab.OVERVIEW,
    val showUpdateDialog: Boolean = false,
    val currentMileageInput: String = "",
    val hasRentingContract: Boolean = false,
    val hudOverlayState: AppOverlayState = AppOverlayState.None,
    val isSwitchingVehicle: Boolean = false,
    val switchingVehicleName: String? = null,
    val isLoading: Boolean = false,
    val error: TextProvider? = null
) : UiState {
    val isNavigationBlocked: Boolean
        get() = hudOverlayState !is AppOverlayState.None || isSwitchingVehicle
    companion object {
        val Empty = State()
    }
}

enum class DashboardTab {
    OVERVIEW,
    HISTORY,
    EXPENSES,
    PROJECTION,
    PROFILE
}

/**
 * Represents the UI events that can be triggered from the Dashboard screen.
 */
sealed interface Event : UiEvent {
    data class OnTabSelected(val tab: DashboardTab) : Event
    data class OnTabSynced(val tab: DashboardTab) : Event
    data object OnOdometerClicked : Event
    data object OnDismissOdometerDialog : Event
    data class OnOdometerChanged(val mileage: String) : Event
    data object ResetToOverview : Event
}

/**
 * Represents the side effects that can be triggered from the Dashboard screen.
 */
sealed interface Effect : UiEffect {
    data class NavigateToTab(val tab: DashboardTab) : Effect
}
