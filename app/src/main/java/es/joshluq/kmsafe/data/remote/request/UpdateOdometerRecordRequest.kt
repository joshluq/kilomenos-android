package es.joshluq.kmsafe.data.remote.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request DTO for updating an existing odometer record.
 */
data class UpdateOdometerRecordRequest(
    @JsonProperty("odometer_value") val odometerValue: Int,
    @JsonProperty("timestamp") val timestamp: String,
    @JsonProperty("label") val label: String? = null,
    @JsonProperty("fuel_amount") val fuelAmount: Double? = null
)
