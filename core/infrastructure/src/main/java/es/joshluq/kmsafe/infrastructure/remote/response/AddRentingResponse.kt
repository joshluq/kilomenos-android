package es.joshluq.kmsafe.infrastructure.remote.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response for adding a new renting contract.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class AddRentingResponse : NetworkResponse() {
    @JsonProperty("contract")
    val contract: RentingContractResponse? = null

    @JsonProperty("initial_record")
    val initialRecord: OdometerRecordResponse? = null
}
