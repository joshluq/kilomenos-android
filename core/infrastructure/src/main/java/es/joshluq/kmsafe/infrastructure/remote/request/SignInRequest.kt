package es.joshluq.kmsafe.infrastructure.remote.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request body for the sign-in endpoint.
 */
data class SignInRequest(
    @JsonProperty("email")
    val email: String,
    @JsonProperty("password")
    val password: String
)
