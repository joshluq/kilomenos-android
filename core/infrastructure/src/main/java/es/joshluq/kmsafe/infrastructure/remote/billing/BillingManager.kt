package es.joshluq.kmsafe.infrastructure.remote.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import es.joshluq.kmsafe.domain.service.BillingService
import es.joshluq.kmsafe.infrastructure.InfrastructureConfig
import es.joshluq.kmsafe.infrastructure.remote.api.BillingApiService
import es.joshluq.kmsafe.infrastructure.remote.auth.UserSessionDataSource
import es.joshluq.kmsafe.infrastructure.remote.request.VerifyPurchaseRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

/**
 * Manages Google Play Billing operations for subscriptions and coordinates
 * backend verification with Supabase Edge Functions.
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: InfrastructureConfig,
    private val apiService: BillingApiService,
    private val sessionDataSource: UserSessionDataSource,
    private val entitlementsRepository: EntitlementsRepository,
    private val logger: LoggerKit
) : PurchasesUpdatedListener, BillingService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    private val _purchaseSuccessFlow = MutableSharedFlow<String>()
    override val purchaseSuccessFlow: SharedFlow<String> = _purchaseSuccessFlow

    private val _purchaseProcessingFlow = MutableSharedFlow<Boolean>()
    override val purchaseProcessingFlow: SharedFlow<Boolean> = _purchaseProcessingFlow

    private val _errorFlow = MutableSharedFlow<String>()
    override val errorFlow: SharedFlow<String> = _errorFlow

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    logger.i("BillingManager", "Billing client setup successful")
                    reconcilePurchasesAtStartup()
                } else {
                    logger.e("BillingManager", "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                logger.w("BillingManager", "Billing service disconnected, retrying...")
                startConnection()
            }
        })
    }

    private fun reconcilePurchasesAtStartup() {
        scope.launch(Dispatchers.IO) {
            try {
                val session = sessionDataSource.getCurrentUserSession()
                if (session == null) {
                    logger.d("BillingManager", "No active user session, skipping startup billing reconciliation")
                    return@launch
                }
                val purchases = queryPurchases()
                val purchasedList = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                if (purchasedList.isNotEmpty()) {
                    logger.i("BillingManager", "Reconciling ${purchasedList.size} active purchases at startup")
                    for (purchase in purchasedList) {
                        handlePurchase(purchase)
                    }
                }
            } catch (e: Exception) {
                logger.w("BillingManager", "Startup purchase reconciliation failed: ${e.message}")
            }
        }
    }

    /**
     * Queries active subscription purchases from Google Play.
     */
    suspend fun queryPurchases(): List<Purchase> = suspendCancellableCoroutine { continuation ->
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                continuation.resume(purchasesList)
            } else {
                logger.e("BillingManager", "Failed to query purchases: ${billingResult.debugMessage}")
                continuation.resume(emptyList())
            }
        }
    }

    /**
     * Launches the billing flow for the premium subscription, linking the active user ID.
     */
    fun launchBillingFlow(activity: Activity) {
        if (config.isDebug) {
            // For internal development, we can simulate success instantly
            // To test real Google Play flow, use License Testers in Play Console.
            logger.i("BillingManager", "DEBUG MODE: Simulating purchase success")
            scope.launch {
                _purchaseProcessingFlow.emit(true)
                delay(300.milliseconds)
                _purchaseSuccessFlow.emit("simulated_order_id")
            }
            return
        }

        scope.launch {
            val currentUserId = sessionDataSource.getCurrentUserSession()?.id

            val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(config.premiumSku)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                )
                .build()

            billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, queryProductDetailsResult ->
                val productDetailsList = queryProductDetailsResult.productDetailsList
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                    val productDetails = productDetailsList[0]
                    val offerToken = productDetails.subscriptionOfferDetails?.getOrNull(0)?.offerToken ?: ""

                    val billingFlowParamsBuilder = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                            listOf(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetails)
                                    .setOfferToken(offerToken)
                                    .build()
                            )
                        )

                    if (!currentUserId.isNullOrBlank()) {
                        billingFlowParamsBuilder.setObfuscatedAccountId(currentUserId)
                    }

                    billingClient.launchBillingFlow(activity, billingFlowParamsBuilder.build())
                } else {
                    logger.e("BillingManager", "Error querying products: ${billingResult.debugMessage}")
                    scope.launch { _errorFlow.emit("Error al conectar con la tienda") }
                }
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK if purchases != null -> {
                val hasPurchased = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                if (hasPurchased) {
                    logger.i(
                        "BillingManager",
                        "Returning from Google Play with PURCHASED state. Emitting purchaseProcessing = true."
                    )
                    scope.launch { _purchaseProcessingFlow.emit(true) }
                }
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                logger.w("BillingManager", "User canceled the purchase")
                scope.launch { _purchaseProcessingFlow.emit(false) }
            }
            else -> {
                logger.e("BillingManager", "Error in purchase update: ${billingResult.debugMessage}")
                scope.launch {
                    _purchaseProcessingFlow.emit(false)
                    _errorFlow.emit(billingResult.debugMessage)
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val subscriptionId = purchase.products.firstOrNull() ?: config.premiumSku
            val request = VerifyPurchaseRequest(
                purchaseToken = purchase.purchaseToken,
                subscriptionId = subscriptionId,
                packageName = context.packageName
            )
            scope.launch(Dispatchers.IO) {
                try {
                    logger.i("BillingManager", "Sending purchase token to backend verification endpoint")
                    val response = withTimeoutOrNull(15_000.milliseconds) {
                        apiService.verifyPurchase(request)
                    }
                    if (response == null) {
                        logger.e("BillingManager", "Backend verification timed out (15s)")
                        _purchaseProcessingFlow.emit(false)
                        _errorFlow.emit("Tiempo de espera agotado al conectar con el servidor.")
                        return@launch
                    }
                    if (response.isSuccessful && response.body()?.success == true) {
                        logger.i("BillingManager", "Purchase successfully verified with backend")
                        withTimeoutOrNull(10_000.milliseconds) {
                            entitlementsRepository.getEntitlements(deviceFingerprint = "", forceRefresh = true).firstOrNull()
                        }
                        val token = purchase.orderId ?: purchase.purchaseToken
                        _purchaseSuccessFlow.emit(token)
                    } else {
                        val errorMsg = response.body()?.error ?: "Error al verificar la suscripción con el servidor"
                        logger.e("BillingManager", "Backend verification rejected: $errorMsg")
                        _purchaseProcessingFlow.emit(false)
                        _errorFlow.emit(errorMsg)
                    }
                } catch (e: Exception) {
                    logger.e("BillingManager", "Exception during purchase verification", e)
                    _purchaseProcessingFlow.emit(false)
                    _errorFlow.emit(e.message ?: "Error de red al verificar la suscripción")
                }
            }
        }
    }
}
