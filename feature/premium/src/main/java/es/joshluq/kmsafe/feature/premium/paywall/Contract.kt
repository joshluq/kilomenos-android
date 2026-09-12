package es.joshluq.kmsafe.feature.premium.paywall

import androidx.compose.runtime.Immutable
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState

/**
 * Available subscription billing plans on the paywall.
 */
enum class PremiumBillingPlan {
    ANNUAL,
    MONTHLY
}

/**
 * Represents the UI state for the Premium Paywall screen.
 */
@Immutable
data class State(
    val isLoading: Boolean = false,
    val isMigrating: Boolean = false,
    val selectedPlan: PremiumBillingPlan = PremiumBillingPlan.ANNUAL,
    val source: String = "general",
    val error: TextProvider? = null
) : UiState {
    companion object {
        val Empty = State()
    }
}

/**
 * Represents the UI events for the Premium Paywall screen.
 */
sealed interface Event : UiEvent {
    data class OnInitialize(val source: String) : Event
    data class OnPlanSelected(val plan: PremiumBillingPlan) : Event
    object OnUpgradeClicked : Event
    object OnDismissClicked : Event
    object OnDismissError : Event
}

/**
 * Represents the side effects for the Premium Paywall screen.
 */
sealed interface Effect : UiEffect {
    object LaunchBillingFlow : Effect
    object NavigateToDashboard : Effect
    object NavigateBack : Effect
}
