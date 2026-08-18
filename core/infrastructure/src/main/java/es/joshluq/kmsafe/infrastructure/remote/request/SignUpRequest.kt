package es.joshluq.kmsafe.infrastructure.remote.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request body for the sign-up endpoint.
 */
data class SignUpRequest(
    @JsonProperty("email")
    val email: String,
    @JsonProperty("password")
    val password: String,
    @JsonProperty("name")
    val name: String? = null
)
