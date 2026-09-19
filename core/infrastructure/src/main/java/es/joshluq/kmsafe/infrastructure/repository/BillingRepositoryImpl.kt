package es.joshluq.kmsafe.infrastructure.repository

import com.android.billingclient.api.Purchase
import es.joshluq.foundationkit.coroutines.DispatcherProvider
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.VerifyPurchaseResult
import es.joshluq.kmsafe.domain.repository.BillingRepository
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import es.joshluq.kmsafe.infrastructure.remote.api.BillingApiService
import es.joshluq.kmsafe.infrastructure.remote.billing.BillingManager
import es.joshluq.kmsafe.infrastructure.remote.request.VerifyPurchaseRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [BillingRepository] interfacing with Google Play Billing via [BillingManager]
 * and the Supabase verification endpoint via [BillingApiService].
 */
@Singleton
class BillingRepositoryImpl @Inject constructor(
    private val apiService: BillingApiService,
    private val billingManager: BillingManager,
    private val entitlementsRepository: EntitlementsRepository,
    private val dispatchers: DispatcherProvider,
    private val logger: LoggerKit
) : BillingRepository {

    override fun verifyPurchase(
        purchaseToken: String,
        subscriptionId: String,
        packageName: String?
    ): Flow<VerifyPurchaseResult> = flow {
        logger.d("BillingRepository", "Verifying purchase with token: $purchaseToken, subscriptionId: $subscriptionId")
        val request = VerifyPurchaseRequest(
            purchaseToken = purchaseToken,
            subscriptionId = subscriptionId,
            packageName = packageName ?: "es.joshluq.kmsafe"
        )
        val response = apiService.verifyPurchase(request)
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.success) {
                logger.i("BillingRepository", "Purchase verified successfully: ${body.message}")
                // Refresh cached remote entitlements to sync session to PREMIUM
                entitlementsRepository.getEntitlements(deviceFingerprint = "", forceRefresh = true).firstOrNull()
                emit(
                    VerifyPurchaseResult(
                        success = true,
                        message = body.message,
                        subscriptionStatus = body.subscription?.status,
                        expiresAt = body.subscription?.expiresAt,
                        autoRenewing = body.subscription?.autoRenewing
                    )
                )
            } else {
                val errorMsg = body?.error ?: body?.message ?: "Purchase verification rejected"
                logger.e("BillingRepository", "Verification failed with message: $errorMsg")
                emit(VerifyPurchaseResult(success = false, message = errorMsg))
            }
        } else {
            val errorMsg = response.errorBody()?.string() ?: "HTTP ${response.code()}"
            logger.e("BillingRepository", "Verification failed with HTTP error: $errorMsg")
            emit(VerifyPurchaseResult(success = false, message = errorMsg))
        }
    }.flowOn(dispatchers.io)

    override fun restorePurchases(): Flow<Int> = flow {
        logger.d("BillingRepository", "Restoring active purchases from Google Play")
        val purchases = billingManager.queryPurchases()
        val purchasedList = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (purchasedList.isEmpty()) {
            logger.i("BillingRepository", "No active purchases found to restore")
            emit(0)
            return@flow
        }

        var restoredCount = 0
        for (purchase in purchasedList) {
            val subscriptionId = purchase.products.firstOrNull() ?: continue
            val result = verifyPurchase(purchase.purchaseToken, subscriptionId).firstOrNull()
            if (result?.success == true) {
                restoredCount++
            }
        }
        logger.i("BillingRepository", "Restoration complete. Restored purchases: $restoredCount")
        emit(restoredCount)
    }.flowOn(dispatchers.io)
}
