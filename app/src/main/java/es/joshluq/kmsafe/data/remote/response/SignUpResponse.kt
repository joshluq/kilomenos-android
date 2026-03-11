package es.joshluq.kmsafe.data.remote.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Success response for the sign-up endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class SignUpResponse : NetworkResponse() {
    @JsonProperty("user")
    val user: UserResponse? = null

    @JsonProperty("subscription_level")
    val subscriptionLevel: String = "FREE"

    @JsonProperty("session")
    val session: SessionResponse? = null
}
