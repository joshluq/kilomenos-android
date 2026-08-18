package es.joshluq.kmsafe.infrastructure.remote.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request model for OAuth authentication.
 */
data class OAuthSignInRequest(
    @JsonProperty("provider") val provider: String,
    @JsonProperty("token") val token: String,
    @JsonProperty("access_token") val accessToken: String? = null,
    @JsonProperty("nonce") val nonce: String? = null
)
