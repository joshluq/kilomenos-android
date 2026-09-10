package es.joshluq.kmsafe.feature.projection

import androidx.compose.runtime.Immutable
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.PlannedTrip
import es.joshluq.kmsafe.domain.model.TripProjection

@Immutable
data class State(
    val isLoading: Boolean = true,
    val isPremium: Boolean = false,

    // Base Contract
    val activeVehicleId: String? = null,
    val totalContractKms: Double = 0.0,
    val startOdometer: Double = 0.0,
    val contractEndDateMillis: Long = 0L,
    val daysRemaining: Long = 0L,
    val penaltyPricePerKm: Float = 0.06f,

    // Baseline Status Quo
    val baselineProjection: TripProjection? = null,
    val realDailyAverage: Float = 0f,

    // Active Simulation
    val simulatedDailyKm: Float = 0f,
    val paceMultiplier: Float = 1.0f,
    val plannedTrips: List<PlannedTrip> = emptyList(),
    val totalPlannedTripsKm: Int = 0,

    // Simulation Outcomes
    val simulatedProjectedTotalKms: Double = 0.0,
    val simulatedFinalBalance: Double = 0.0,
    val estimatedPenalty: Double = 0.0,
    val exhaustionDateMillis: Long? = null,
    val monthsAheadOrBehind: Int = 0,
    val remedialDailyKm: Double? = null,

    // Financial Settlement & Courtesy Breakdown
    val grossExcessKms: Double = 0.0,
    val courtesyMarginKms: Double = 0.0,
    val billableExcessKms: Double = 0.0,
    val ratePerKm: Float = 0.06f,
    val courtesySavingsAmount: Double = 0.0,
    val isUsingDefaultPrice: Boolean = false,
    val isUsingDefaultCourtesyMargin: Boolean = false,

    val error: TextProvider? = null
) : UiState {
    val isOverLimit: Boolean get() = simulatedFinalBalance < 0.0

    companion object {
        val Empty = State()
    }
}

sealed interface Event : UiEvent {
    data class OnSimulatedKmChanged(val newValue: Float) : Event
    data class OnPacePresetSelected(val multiplier: Float) : Event
    data class OnAddPresetTrip(val title: String, val distanceKms: Int) : Event
    data class OnRemoveTrip(val tripId: String) : Event
    data class OnCustomTripChanged(val distanceKms: Int) : Event
    data class OnPenaltyPriceChanged(val newPrice: Float) : Event
    data object OnResetSimulation : Event
    data object OnConfigureContractClicked : Event
    data object OnUpgradeToPremiumClicked : Event
    data object OnDismissError : Event
}

sealed interface Effect : UiEffect {
    data object NavigateToPremiumPaywall : Effect
    data class NavigateToEditContract(val vehicleId: String) : Effect
    data class ShowToast(val message: TextProvider) : Effect
}
