package es.joshluq.kmsafe.ui.projection

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.TripProjection

data class State(
    val isLoading: Boolean = true,
    val baselineProjection: TripProjection? = null,
    val simulatedDailyKm: Float = 0f,
    val currentRealDailyAverage: Float = 0f,
    val simulatedFinalBalance: Int = 0,
    val penaltyPricePerKm: Float = 0.05f, // Default 5 cents
    val estimatedPenalty: Double = 0.0,
    val exhaustionDate: Long? = null,
    val daysRemaining: Long = 0,
    val totalContractKms: Int = 0,
    val plannedTripKms: Int = 0,
    val recommendedDailyKm: Int? = null,
    val error: TextProvider? = null
) : UiState {
    companion object {
        val Empty = State()
    }
}

sealed interface Event : UiEvent {
    data class OnSimulatedKmChanged(val newValue: Float) : Event
    data class OnPenaltyPriceChanged(val newValue: Float) : Event
    data class OnPlannedTripChanged(val newValue: Int) : Event
    data object OnDismissError : Event
}

sealed interface Effect : UiEffect
