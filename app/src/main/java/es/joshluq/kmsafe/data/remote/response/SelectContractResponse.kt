package es.joshluq.kmsafe.data.remote.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response for the contract selection endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class SelectContractResponse : NetworkResponse() {
    @JsonProperty("contract")
    val contract: RentingContractResponse? = null
}
