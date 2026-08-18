package es.joshluq.kmsafe.infrastructure.remote.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Success response for the sign-in endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class SignInResponse : NetworkResponse() {
    @JsonProperty("user")
    val user: UserResponse? = null

    @JsonProperty("subscription_level")
    val subscriptionLevel: String? = null

    @JsonProperty("session")
    val session: SessionResponse? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class UserResponse {
    @JsonProperty("id")
    val id: String? = null

    @JsonProperty("email")
    val email: String? = null

    @JsonProperty("user_metadata")
    val userMetadata: UserMetadataResponse? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class UserMetadataResponse {
    @JsonProperty("name")
    val name: String? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class SessionResponse {
    @JsonProperty("access_token")
    val accessToken: String? = null

    @JsonProperty("refresh_token")
    val refreshToken: String? = null
}
