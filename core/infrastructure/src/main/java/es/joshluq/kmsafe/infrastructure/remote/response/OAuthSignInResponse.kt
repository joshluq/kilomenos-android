package es.joshluq.kmsafe.infrastructure.remote.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response model for OAuth authentication.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class OAuthSignInResponse(
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("message") val message: String?,
    @JsonProperty("user") val user: UserResponse?,
    @JsonProperty("subscription_level") val subscriptionLevel: String?,
    @JsonProperty("session") val session: SessionResponse?,
    @JsonProperty("error") val error: String? = null
)
