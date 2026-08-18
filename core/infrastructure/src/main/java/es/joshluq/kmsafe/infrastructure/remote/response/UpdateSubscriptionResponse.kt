package es.joshluq.kmsafe.infrastructure.remote.response

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response body for a subscription update request.
 */
data class UpdateSubscriptionResponse(
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("message") val message: String,
    @JsonProperty("subscription_level") val subscriptionLevel: String
)
