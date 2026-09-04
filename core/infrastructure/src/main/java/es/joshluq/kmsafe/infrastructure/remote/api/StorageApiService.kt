package es.joshluq.kmsafe.infrastructure.remote.api

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit service for Supabase Storage operations.
 */
interface StorageApiService {

    /**
     * Uploads a binary file to the vehicle-images bucket.
     * Uses PUT to allow overwriting existing files (upsert behavior).
     */
    @PUT("/storage/v1/object/vehicle-images/contratos/{path}")
    suspend fun uploadVehicleImage(
        @Path("path") path: String,
        @Body image: RequestBody,
        @Header("x-upsert") upsert: String = "true"
    ): Response<Unit>

    /**
     * Uploads a binary file to the receipts bucket.
     * Uses PUT to allow overwriting existing files (upsert behavior).
     */
    @PUT("/storage/v1/object/receipts/{userId}/{fileName}")
    suspend fun uploadReceipt(
        @Path("userId") userId: String,
        @Path("fileName") fileName: String,
        @Body image: RequestBody,
        @Header("x-upsert") upsert: String = "true"
    ): Response<Unit>

    /**
     * Deletes a file from the receipts bucket.
     */
    @DELETE("/storage/v1/object/receipts/{userId}/{fileName}")
    suspend fun deleteReceipt(
        @Path("userId") userId: String,
        @Path("fileName") fileName: String
    ): Response<Unit>
}
