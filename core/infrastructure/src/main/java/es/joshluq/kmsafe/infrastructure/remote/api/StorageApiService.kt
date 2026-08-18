package es.joshluq.kmsafe.infrastructure.remote.api

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit service for Supabase Storage operations.
 */
interface StorageApiService {

    /**
     * Uploads a binary file to the vehicle-images bucket.
     */
    @POST("/storage/v1/object/vehicle-images/contratos/{path}")
    suspend fun uploadVehicleImage(
        @Path("path") path: String,
        @Body image: RequestBody
    ): Response<Unit>
}
