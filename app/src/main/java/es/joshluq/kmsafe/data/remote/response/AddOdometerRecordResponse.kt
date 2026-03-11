package es.joshluq.kmsafe.data.remote.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response for the odometer record creation endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class AddOdometerRecordResponse : NetworkResponse() {
    @JsonProperty("record")
    val record: OdometerRecordResponse? = null
}
