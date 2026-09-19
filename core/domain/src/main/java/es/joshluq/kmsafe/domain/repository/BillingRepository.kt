package es.joshluq.kmsafe.domain.repository

import es.joshluq.kmsafe.domain.model.VerifyPurchaseResult
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for in-app billing verification and purchase reconciliation.
 */
interface BillingRepository {

    /**
     * Verifies a Google Play subscription purchase with the backend service.
     *
     * @param purchaseToken The purchase token issued by Google Play.
     * @param subscriptionId The product SKU/subscription ID (e.g., subscription_premium_monthly).
     * @param packageName The application package name. Defaults to null to let the service apply default.
     * @return Flow emitting [VerifyPurchaseResult].
     */
    fun verifyPurchase(
        purchaseToken: String,
        subscriptionId: String,
        packageName: String? = null
    ): Flow<VerifyPurchaseResult>

    /**
     * Reconciles and restores active Google Play subscriptions.
     * Queries active purchases from Google Play, verifies them with the backend,
     * and updates user entitlements.
     *
     * @return Flow emitting the count of successfully restored and verified subscriptions.
     */
    fun restorePurchases(): Flow<Int>
}
