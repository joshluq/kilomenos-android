package es.joshluq.kmsafe.data.remote.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request body for adding an odometer record.
 */
data class AddOdometerRecordRequest(
    @JsonProperty("timestamp")
    val timestamp: String,
    @JsonProperty("odometer_value")
    val odometerValue: Int,
    @JsonProperty("label")
    val label: String? = null,
    @JsonProperty("fuel_amount")
    val fuelAmount: Double? = null
)
