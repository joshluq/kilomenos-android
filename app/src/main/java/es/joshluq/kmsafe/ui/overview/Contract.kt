package es.joshluq.kmsafe.ui.overview

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.TripProjection
import es.joshluq.kmsafe.ui.overview.model.MonthlyUsageUiModel

/**
 * Represents the UI state for the Overview screen.
 */
data class State(
    val renting: RentingContract? = null,
    val balance: Int = 0,
    val dailyLimit: Int = 0,
    val monthlyLimit: Int = 0,
    val totalKmsDriven: Int = 0,
    val timePercentage: Float = 0f,
    val kmsPercentage: Float = 0f,
    val differencePercentage: Float = 0f,
    val monthlyUsage: List<MonthlyUsageUiModel> = emptyList(),
    val showBottomSheet: Boolean = false,
    val newOdometerValue: String = "",
    val newRecordDate: Long = System.currentTimeMillis(),
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val projection: TripProjection? = null,
    val showProjectionBanner: Boolean = false,
    val availableVehicles: List<RentingContract> = emptyList(),
    val showVehicleSwitcher: Boolean = false,
    val isPremium: Boolean? = null,
    val subscriptionLevel: SubscriptionLevel = SubscriptionLevel.FREE,
    val newRecordLabel: String = "",
    val newRecordFuel: String = "",
    val isSyncPending: Boolean = false,
    val isTracking: Boolean = false,
    val autoTrackingEnabled: Boolean = false,
    val isAutoTrackingTrialable: Boolean = false,
    val autoTrackingPromotionDismissed: Boolean = false,
    val showAutoTrackingPromotion: Boolean = false,
    val trackedDistance: Double = 0.0,
    val tripStartTime: Long? = null,
    val error: TextProvider? = null
) : UiState {

    companion object {
        val Empty = State()
    }

    val hasRenting: Boolean
        get() = renting != null
}

/**
 * Represents the UI events that can be triggered from the Overview screen.
 */
sealed interface Event : UiEvent {
    data object OnRegisterRentingClicked : Event
    data class OnEditContractClicked(val id: String) : Event
    data object OnUpdateOdometerClicked : Event
    data object OnBottomSheetDismissed : Event
    data object OnDismissProjectionBanner : Event
    data object OnProjectionBannerClicked : Event
    data object OnToggleVehicleSwitcher : Event
    data class OnNewOdometerChanged(val value: String) : Event
    data class OnNewLabelChanged(val value: String) : Event
    data class OnNewFuelChanged(val value: String) : Event
    data class OnSaveRecordClicked(val timestamp: Long) : Event
    data class OnSwitchVehicleClicked(val id: String) : Event
    data object OnDismissError : Event
    data object OnStartTrackingClicked : Event
    data object OnStopTrackingClicked : Event
    data object OnConfirmTrackedTripClicked : Event
    data object OnCancelTrackedTripClicked : Event
    data object OnRequestPermissionsRationale : Event
    data object OnPermissionsRationaleSuccess : Event
    data object OnPremiumUpgradeClicked : Event
    data object OnDismissAutoTrackingPromotion : Event
    data object OnAutoTrackingPromotionAccepted : Event
    data class OnAutoTrackingToggled(val enabled: Boolean) : Event
    data object OnWelcomeGuideClicked : Event
}

/**
 * Represents the side effects that can occur on the Overview screen.
 */
sealed interface Effect : UiEffect {
    data class NavigateToOnboarding(
        val vehicleId: String? = null,
        val isEdit: Boolean = false
    ) : Effect
    data object NavigateToProjection : Effect
    data object StartTrackingService : Effect
    data object StopTrackingService : Effect
    data object OpenAppSettings : Effect
    data object NavigateToPermissions : Effect
    data object NavigateToPremiumPaywall : Effect
    data object NavigateToPreferences : Effect
    data object NavigateToWelcomeDiscovery : Effect
}
