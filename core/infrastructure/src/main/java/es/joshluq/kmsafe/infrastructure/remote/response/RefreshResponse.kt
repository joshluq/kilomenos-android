package es.joshluq.kmsafe.infrastructure.remote.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response for the token refresh endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class RefreshResponse : NetworkResponse() {
    @JsonProperty("session")
    val session: SessionResponse? = null
}
