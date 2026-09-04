package es.joshluq.kmsafe.infrastructure.remote.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response for the list of fuel expenses.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class FuelExpenseListResponse : NetworkResponse() {
    @JsonProperty("expenses")
    val expenses: List<FuelExpenseRemoteModel>? = null
}

/**
 * Response for a single fuel expense.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class FuelExpenseResponse : NetworkResponse() {
    @JsonProperty("expense")
    val expense: FuelExpenseRemoteModel? = null
}

/**
 * Remote model for a Fuel Expense.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class FuelExpenseRemoteModel {
    @JsonProperty("id")
    val id: String? = null

    @JsonProperty("contract_id")
    val contractId: String? = null

    @JsonProperty("timestamp")
    val timestamp: String? = null

    @JsonProperty("odometer_value")
    val odometerValue: Double? = null

    @JsonProperty("volume")
    val volume: Double? = null

    @JsonProperty("price_per_unit")
    val pricePerUnit: Double? = null

    @JsonProperty("total_cost")
    val totalCost: Double? = null

    @JsonProperty("is_full_tank")
    val isFullTank: Boolean? = null

    @JsonProperty("fuel_type")
    val fuelType: String? = null

    @JsonProperty("station_id")
    val stationId: String? = null

    @JsonProperty("station_name")
    val stationName: String? = null

    @JsonProperty("km_since_last_refuel")
    val kmSinceLastRefuel: Double? = null

    @JsonProperty("consumption_per_100km")
    val consumptionPer100km: Double? = null

    @JsonProperty("receipt_image_path")
    val receiptImagePath: String? = null
}
