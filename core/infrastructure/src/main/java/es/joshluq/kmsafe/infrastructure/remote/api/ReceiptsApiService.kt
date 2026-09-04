package es.joshluq.kmsafe.infrastructure.remote.api

import es.joshluq.kmsafe.infrastructure.remote.dto.ProcessReceiptRequest
import es.joshluq.kmsafe.infrastructure.remote.dto.ProcessReceiptResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service for Supabase Edge Functions processing receipt OCR via Gemini.
 */
interface ReceiptsApiService {

    /**
     * Processes a receipt image stored in Supabase Storage.
     */
    @POST("/functions/v1/process-receipt")
    suspend fun processReceipt(
        @Body request: ProcessReceiptRequest
    ): Response<ProcessReceiptResponse>
}
