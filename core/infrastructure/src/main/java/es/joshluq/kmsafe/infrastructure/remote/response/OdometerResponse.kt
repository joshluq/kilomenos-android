package es.joshluq.kmsafe.infrastructure.remote.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response for the list of odometer records for a specific contract.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class OdometerRecordListResponse : NetworkResponse() {
    @JsonProperty("records")
    val records: List<OdometerRecordResponse>? = null
}

/**
 * Remote model for an Odometer Record.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class OdometerRecordResponse {
    @JsonProperty("id")
    val id: String? = null

    @JsonProperty("contract_id")
    val contractId: String? = null

    @JsonProperty("timestamp")
    val timestamp: String? = null

    @JsonProperty("odometer_value")
    val odometerValue: Int? = null

    @JsonProperty("is_initial_record")
    val isInitialRecord: Boolean? = null

    @JsonProperty("label")
    val label: String? = null

    @JsonProperty("fuel_consumed")
    val fuelConsumed: Double? = null

    @JsonProperty("has_route")
    val hasRoute: Boolean? = null
}
