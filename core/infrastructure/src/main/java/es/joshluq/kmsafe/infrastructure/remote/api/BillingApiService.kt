package es.joshluq.kmsafe.infrastructure.remote.api

import es.joshluq.kmsafe.infrastructure.remote.request.VerifyPurchaseRequest
import es.joshluq.kmsafe.infrastructure.remote.response.VerifyPurchaseResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service for Google Play in-app purchase verification with Supabase Edge Functions.
 */
interface BillingApiService {

    /**
     * Verifies and links a Google Play subscription purchase with the active user session.
     */
    @POST("v1/purchases/google-play/verify")
    suspend fun verifyPurchase(
        @Body request: VerifyPurchaseRequest
    ): Response<VerifyPurchaseResponse>
}
