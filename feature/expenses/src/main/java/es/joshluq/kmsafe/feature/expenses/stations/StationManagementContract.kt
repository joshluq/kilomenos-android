package es.joshluq.kmsafe.feature.expenses.stations

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.ServiceStation

/**
 * UI state for the Station Management screen.
 */
data class StationManagementState(
    val stations: List<ServiceStation> = emptyList(),
    val filteredStations: List<ServiceStation> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: TextProvider? = null,
    val isPremium: Boolean = false,
    val currentLat: Double? = null,
    val currentLng: Double? = null,
    val selectedStation: ServiceStation? = null,
    val isEditSheetOpen: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val deleteTargetId: String? = null
) : UiState {
    companion object {
        val Empty = StationManagementState()
    }
}

/**
 * UI events for Station Management.
 */
sealed interface StationManagementEvent : UiEvent {
    data object OnRefresh : StationManagementEvent
    data class OnSearchQueryChanged(val query: String) : StationManagementEvent
    data class OnToggleFavorite(val stationId: String, val isFavorite: Boolean) : StationManagementEvent
    data class OnDeleteStation(val stationId: String) : StationManagementEvent
    data object OnConfirmDeleteStation : StationManagementEvent
    data object OnCancelDeleteStation : StationManagementEvent
    data class OnLocationCaptured(val latitude: Double, val longitude: Double) : StationManagementEvent
    data class OnEditStation(val station: ServiceStation) : StationManagementEvent
    data object OnAddStationClicked : StationManagementEvent
    data class OnViewDetail(val stationId: String) : StationManagementEvent
    data object OnDismissEdit : StationManagementEvent
    data class OnSaveStation(
        val id: String?,
        val name: String,
        val brand: String
    ) : StationManagementEvent
    data object OnDismissError : StationManagementEvent
}

/**
 * Side effects for Station Management.
 */
sealed interface StationManagementEffect : UiEffect {
    data object NavigateBack : StationManagementEffect
    data class NavigateToDetail(val stationId: String) : StationManagementEffect
}
