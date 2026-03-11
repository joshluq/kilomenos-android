package es.joshluq.kmsafe.data.remote.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request body for the token refresh endpoint.
 */
data class RefreshRequest(
    @JsonProperty("refresh_token")
    val refreshToken: String
)
