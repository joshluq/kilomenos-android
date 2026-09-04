package es.joshluq.kmsafe.infrastructure.remote.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Remote request model for a Fuel Expense.
 */
data class FuelExpenseRequest(
    @JsonProperty("id") val id: String,
    @JsonProperty("timestamp") val timestamp: String,
    @JsonProperty("odometer_value") val odometerValue: Double? = null,
    @JsonProperty("volume") val volumeQuantity: Double,
    @JsonProperty("price_per_unit") val pricePerUnit: Double,
    @JsonProperty("total_cost") val totalCost: Double,
    @JsonProperty("is_full_tank") val isFullTank: Boolean,
    @JsonProperty("fuel_type") val fuelType: String,
    @JsonProperty("station_id") val stationId: String? = null,
    @JsonProperty("station_name") val stationName: String? = null,
    @JsonProperty("km_since_last_refuel") val kmSinceLastRefuel: Double? = null,
    @JsonProperty("consumption_per_100km") val consumptionPer100km: Double? = null,
    @JsonProperty("receipt_image_path") val receiptImagePath: String? = null
)
