package es.joshluq.kmsafe.domain.model

/**
 * Domain model representing a fuel or EV charging expense record.
 *
 * @property id Unique UUID v4 identifying the expense entry.
 * @property vehicleId ID of the vehicle contract associated with this expense.
 * @property stationId Optional reference to the physical [ServiceStation].
 * @property stationName Display name of the station or location where refueling/charging occurred.
 * @property timestamp Epoch timestamp (in milliseconds) when the expense took place.
 * @property fuelType Energy or fuel type consumed.
 * @property unitPrice Price per unit (€/L, €/kWh, or €/kg).
 * @property volumeQuantity Quantity consumed (Liters, kWh, or kg).
 * @property totalCost Total monetary amount (€) paid.
 * @property odometerAtExpense Optional vehicle odometer reading recorded at the time of expense.
 * @property isFullTank True if refueling was completed until full tank / full charge.
 * @property notes Optional custom user notes for the expense.
 * @property kmSinceLastRefuel Total kilometers driven since the previous full-tank refuel,
 *   automatically inferred from [OdometerRecord] history. Null if this is the first refuel
 *   or if insufficient data exists.
 * @property consumptionPer100km Calculated fuel or energy consumption per 100 km (L/100km or kWh/100km).
 *   Only populated when [isFullTank] is true and [kmSinceLastRefuel] is available.
 * @property receiptImagePath Optional relative path in Supabase Storage pointing to the audited receipt image.
 * @property syncStatus Offline-first synchronization status.
 */
data class FuelExpense(
    val id: String,
    val vehicleId: String,
    val stationId: String? = null,
    val stationName: String? = null,
    val timestamp: Long,
    val fuelType: FuelType,
    val unitPrice: Double,
    val volumeQuantity: Double,
    val totalCost: Double,
    val odometerAtExpense: Double? = null,
    val isFullTank: Boolean = true,
    val notes: String? = null,
    val kmSinceLastRefuel: Double? = null,
    val consumptionPer100km: Double? = null,
    val receiptImagePath: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
