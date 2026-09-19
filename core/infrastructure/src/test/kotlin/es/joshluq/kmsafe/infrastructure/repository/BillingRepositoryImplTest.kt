package es.joshluq.kmsafe.infrastructure.repository

import com.android.billingclient.api.Purchase
import es.joshluq.foundationkit.coroutines.DispatcherProvider
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import es.joshluq.kmsafe.infrastructure.remote.api.BillingApiService
import es.joshluq.kmsafe.infrastructure.remote.billing.BillingManager
import es.joshluq.kmsafe.infrastructure.remote.request.VerifyPurchaseRequest
import es.joshluq.kmsafe.infrastructure.remote.response.VerifyPurchaseResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class BillingRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val apiService: BillingApiService = mockk()
    private val billingManager: BillingManager = mockk()
    private val entitlementsRepository: EntitlementsRepository = mockk(relaxed = true)
    private val dispatchers: DispatcherProvider = mockk()
    private val logger: LoggerKit = mockk(relaxed = true)

    private lateinit var repository: BillingRepositoryImpl

    @Before
    fun setup() {
        every { dispatchers.io } returns testDispatcher
        repository = BillingRepositoryImpl(
            apiService = apiService,
            billingManager = billingManager,
            entitlementsRepository = entitlementsRepository,
            dispatchers = dispatchers,
            logger = logger
        )
    }

    @Test
    fun `verifyPurchase returns success and refreshes entitlements when api succeeds`() = runTest(testDispatcher) {
        val response = VerifyPurchaseResponse(
            success = true,
            message = "Compra verificada"
        )
        coEvery {
            apiService.verifyPurchase(
                VerifyPurchaseRequest(
                    purchaseToken = "token-1",
                    subscriptionId = "subscription_premium_monthly",
                    packageName = "es.joshluq.kmsafe"
                )
            )
        } returns Response.success(response)
        every { entitlementsRepository.getEntitlements(any(), any()) } returns flowOf(Entitlements.Default)

        val result = repository.verifyPurchase("token-1", "subscription_premium_monthly").first()

        assertTrue(result.success)
        assertEquals("Compra verificada", result.message)
        coVerify(exactly = 1) { entitlementsRepository.getEntitlements("", true) }
    }

    @Test
    fun `verifyPurchase returns failure when api returns error response`() = runTest(testDispatcher) {
        coEvery {
            apiService.verifyPurchase(any())
        } returns Response.error(400, """{"error":"Invalid token"}""".toResponseBody())

        val result = repository.verifyPurchase("token-1", "subscription_premium_monthly").first()

        assertFalse(result.success)
    }

    @Test
    fun `restorePurchases queries active purchases and returns restored count`() = runTest(testDispatcher) {
        val purchaseMock = mockk<Purchase> {
            every { purchaseState } returns Purchase.PurchaseState.PURCHASED
            every { products } returns listOf("subscription_premium_monthly")
            every { purchaseToken } returns "token-restore-1"
        }
        coEvery { billingManager.queryPurchases() } returns listOf(purchaseMock)

        val verifyResponse = VerifyPurchaseResponse(
            success = true,
            message = "Restored"
        )
        coEvery { apiService.verifyPurchase(any()) } returns Response.success(verifyResponse)
        every { entitlementsRepository.getEntitlements(any(), any()) } returns flowOf(Entitlements.Default)

        val restoredCount = repository.restorePurchases().first()

        assertEquals(1, restoredCount)
    }

    @Test
    fun `restorePurchases returns zero when no active purchases found`() = runTest(testDispatcher) {
        coEvery { billingManager.queryPurchases() } returns emptyList()

        val restoredCount = repository.restorePurchases().first()

        assertEquals(0, restoredCount)
    }
}
