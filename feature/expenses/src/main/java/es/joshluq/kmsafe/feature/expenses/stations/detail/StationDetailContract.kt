package es.joshluq.kmsafe.feature.expenses.stations.detail

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.ServiceStationDetail
import es.joshluq.kmsafe.domain.model.StationPriceVolatility

/**
 * UI state for the Station Detail screen.
 */
data class StationDetailState(
    val stationId: String = "",
    val detail: ServiceStationDetail? = null,
    val volatility: StationPriceVolatility? = null,
    val isLoading: Boolean = false,
    val isPremium: Boolean = false,
    val error: TextProvider? = null,
    val showVolatilityChart: Boolean = true
) : UiState {
    companion object {
        val Empty = StationDetailState()
    }
}

/**
 * UI events for Station Detail.
 */
sealed interface StationDetailEvent : UiEvent {
    data object OnRefresh : StationDetailEvent
    data class OnToggleFavorite(val isFavorite: Boolean) : StationDetailEvent
    data object OnDismissError : StationDetailEvent
}

/**
 * Side effects for Station Detail.
 */
sealed interface StationDetailEffect : UiEffect {
    data object NavigateBack : StationDetailEffect
}
