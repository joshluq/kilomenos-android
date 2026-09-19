package es.joshluq.kmsafe.infrastructure.remote.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response received when verifying a Google Play purchase.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class VerifyPurchaseResponse(
    @JsonProperty("success")
    val success: Boolean = false,
    @JsonProperty("message")
    val message: String? = null,
    @JsonProperty("error")
    val error: String? = null,
    @JsonProperty("subscription")
    val subscription: SubscriptionDetailsResponse? = null
)

/**
 * Subscription details returned within [VerifyPurchaseResponse].
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SubscriptionDetailsResponse(
    @JsonProperty("status")
    val status: String? = null,
    @JsonProperty("expires_at")
    val expiresAt: String? = null,
    @JsonProperty("auto_renewing")
    val autoRenewing: Boolean? = null
)
