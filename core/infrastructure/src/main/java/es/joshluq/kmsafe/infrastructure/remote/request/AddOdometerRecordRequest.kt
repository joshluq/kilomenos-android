package es.joshluq.kmsafe.infrastructure.remote.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request body for adding an odometer record.
 */
data class AddOdometerRecordRequest(
    @JsonProperty("id")
    val id: String,
    @JsonProperty("timestamp")
    val timestamp: String,
    @JsonProperty("odometer_value")
    val odometerValue: Int,
    @JsonProperty("label")
    val label: String? = null,
    @JsonProperty("fuel_consumed")
    val fuelConsumed: Double? = null
)
