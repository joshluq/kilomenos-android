package es.joshluq.kmsafe.data.remote.api

import es.joshluq.kmsafe.data.remote.response.EntitlementsResponse
import es.joshluq.kmsafe.data.remote.response.StartTrialResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit service for user entitlements and trial management.
 */
interface EntitlementsApiService {

    @GET("v1/user/entitlements")
    suspend fun getEntitlements(
        @Header("x-device-fingerprint") fingerprint: String
    ): Response<EntitlementsResponse>

    @POST("v1/user/trial/start")
    suspend fun startTrial(
        @Body request: StartTrialRequest
    ): Response<StartTrialResponse>
}

/**
 * Request body for starting a trial.
 */
data class StartTrialRequest(
    val device_fingerprint: String
)
