package es.joshluq.kmsafe.infrastructure.remote.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request body for updating the user's subscription level.
 */
data class UpdateSubscriptionRequest(
    @JsonProperty("level") val level: String
)
