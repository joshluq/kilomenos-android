package es.joshluq.kmsafe.domain.model

/**
 * Domain entity representing the result of a Google Play purchase verification.
 *
 * @property success Whether the purchase was successfully validated and linked by the backend.
 * @property message Informative message returned by the service.
 * @property subscriptionStatus Google Play subscription state (e.g., SUBSCRIPTION_STATE_ACTIVE).
 * @property expiresAt Expiration timestamp of the subscription in ISO-8601 format.
 * @property autoRenewing Whether the subscription is configured for automatic renewal.
 */
data class VerifyPurchaseResult(
    val success: Boolean,
    val message: String? = null,
    val subscriptionStatus: String? = null,
    val expiresAt: String? = null,
    val autoRenewing: Boolean? = null
)
