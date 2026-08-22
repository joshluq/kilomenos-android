package es.joshluq.kmsafe.feature.expenses

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.domain.model.StationPriceVolatility

/**
 * Filter mode for expenses.
 */
enum class ExpenseFilterMode {
    ALL,
    COMBUSTION,
    ELECTRIC
}

/**
 * UI state for the Expenses screen.
 */
data class ExpensesState(
    val vehicleId: String? = null,
    val vehicleName: String? = null,
    val expenses: List<FuelExpense> = emptyList(),
    val filteredExpenses: List<FuelExpense> = emptyList(),
    val filterMode: ExpenseFilterMode = ExpenseFilterMode.ALL,
    val currentMonthTotalCost: Double = 0.0,
    val currentMonthTotalVolume: Double = 0.0,
    val allTimeTotalCost: Double = 0.0,
    val electrificationSavingsEuros: Double = 0.0,
    val isAddExpenseSheetOpen: Boolean = false,
    val selectedVolatility: StationPriceVolatility? = null,
    val isPremium: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val initialStationId: String? = null,
    val priceReportMode: Boolean = false,
    val currentLat: Double? = null,
    val currentLng: Double? = null,
    val vehicleFuelType: FuelType = FuelType.GASOLINE_95,
    val stations: List<ServiceStation> = emptyList(),
    val error: TextProvider? = null,
    val successMessage: TextProvider? = null,
    /** Real-time odometer pre-filled in the Add Expense form (startOdometer + ∑ OdometerRecords). */
    val currentOdometer: Double = 0.0,
    /** Timestamp of the last full-tank refuel; bounds the km window for the A+C algorithm. */
    val lastRefuelTimestamp: Long? = null,
    /** Total km driven since the [lastRefuelTimestamp]. */
    val kmSinceLastFullRefuel: Double? = null,
    /** List of odometer records recorded since the [lastRefuelTimestamp]. */
    val recordsSinceLastRefuel: List<OdometerRecord> = emptyList(),
    /** Whether the list of trips since last refuel is currently visible. */
    val showTripsSinceLastRefuel: Boolean = false,
    /** Fuel type inferred from the most recent expense. Defaults to GASOLINE_95 if no history. */
    val lastUsedFuelType: FuelType = FuelType.GASOLINE_95,
    /** Unit price from the most recent expense of the same fuel type. Used to pre-fill the price field. */
    val lastUnitPrice: Double? = null,
    /** Consumption result to display in the post-save banner. Null when not yet available. */
    val consumptionBannerData: ConsumptionBannerData? = null,
    val showDeleteConfirmation: Boolean = false,
    val deleteTargetId: String? = null
) : UiState {
    companion object {
        val Empty = ExpensesState()
    }
}

/**
 * Data for the post-save consumption banner shown after a full-tank refuel.
 *
 * @property consumptionPer100km Calculated consumption (L or kWh per 100 km).
 * @property unit Unit string (L or kWh).
 * @property averageConsumption Historical average for comparison, null if insufficient data.
 */
data class ConsumptionBannerData(
    val consumptionPer100km: Double,
    val unit: String,
    val averageConsumption: Double?
)

/**
 * UI events handled by the Expenses ViewModel.
 */
sealed interface ExpensesEvent : UiEvent {
    data object OnRefresh : ExpensesEvent
    data class OnFilterChanged(val mode: ExpenseFilterMode) : ExpensesEvent
    data object OnOpenAddExpense : ExpensesEvent
    data object OnDismissAddExpense : ExpensesEvent
    data class OnLocationCaptured(val latitude: Double, val longitude: Double) : ExpensesEvent
    data object OnManageStationsClicked : ExpensesEvent
    data class OnDeleteExpense(val expenseId: String) : ExpensesEvent
    data object OnConfirmDeleteExpense : ExpensesEvent
    data object OnCancelDeleteExpense : ExpensesEvent
    data class OnSaveExpense(
        val fuelType: FuelType,
        val unitPrice: Double,
        val volumeQuantity: Double,
        val totalCost: Double,
        val stationId: String?,
        val stationName: String?,
        val odometerAtExpense: Double?,
        val isFullTank: Boolean,
        val notes: String?,
        /** Forwarded from [ExpensesState.lastRefuelTimestamp] to bound the A+C km window. */
        val lastRefuelTimestamp: Long?
    ) : ExpensesEvent
    data class OnViewStationVolatility(val stationId: String, val fuelType: FuelType) : ExpensesEvent
    data class OnViewStationDetail(val stationId: String) : ExpensesEvent
    data object OnDismissVolatility : ExpensesEvent
    data object OnDismissError : ExpensesEvent
    data object OnDismissSuccess : ExpensesEvent
    data object OnToggleTripsVisibility : ExpensesEvent
    data object OnDismissConsumptionBanner : ExpensesEvent
}

/**
 * Side effects triggered by the Expenses ViewModel.
 */
sealed interface ExpensesEffect : UiEffect {
    data object NavigateToUpgrade : ExpensesEffect
    data object NavigateToStations : ExpensesEffect
    data class NavigateToStationDetail(val stationId: String) : ExpensesEffect
}
