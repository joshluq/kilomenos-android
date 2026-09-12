package es.joshluq.kmsafe.domain.model

import kotlinx.serialization.Serializable

/**
 * Immutable Value Object representing the glanceable summary data of the active vehicle contract
 * displayed on the Android Home Screen Widget.
 *
 * @param vehicleName Name or model of the active vehicle.
 * @param balance Updated mileage balance in km (positive = surplus, negative = excess penalty risk).
 * @param currentOdometer Consolidated odometer reading of the vehicle.
 * @param dailyBudget Base daily kilometer allowance.
 * @param isSafe True if the balance is zero or positive (safe zone).
 * @param isSyncPending True if there are offline records awaiting synchronization.
 * @param vehiclePlate Optional vehicle license plate.
 */
@Serializable
data class WidgetSummary(
    val vehicleName: String,
    val balance: Double,
    val currentOdometer: Double,
    val dailyBudget: Double,
    val isSafe: Boolean,
    val isSyncPending: Boolean
)
