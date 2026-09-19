package es.joshluq.kmsafe.infrastructure.remote.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request payload for verifying a Google Play purchase with Supabase backend.
 */
data class VerifyPurchaseRequest(
    @JsonProperty("purchase_token")
    val purchaseToken: String,
    @JsonProperty("subscription_id")
    val subscriptionId: String,
    @JsonProperty("package_name")
    val packageName: String = "es.joshluq.kmsafe"
)
