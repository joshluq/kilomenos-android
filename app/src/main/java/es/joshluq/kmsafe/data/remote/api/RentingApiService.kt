package es.joshluq.kmsafe.data.remote.api

import es.joshluq.kmsafe.data.remote.request.AddOdometerRecordRequest
import es.joshluq.kmsafe.data.remote.request.CreateRentingContractRequest
import es.joshluq.kmsafe.data.remote.request.UpdateOdometerRecordRequest
import es.joshluq.kmsafe.data.remote.request.UpdateRentingContractRequest
import es.joshluq.kmsafe.data.remote.request.UploadRouteRequest
import es.joshluq.kmsafe.data.remote.response.AddOdometerRecordResponse
import es.joshluq.kmsafe.data.remote.response.AddRentingResponse
import es.joshluq.kmsafe.data.remote.response.OdometerRecordListResponse
import es.joshluq.kmsafe.data.remote.response.RentingListResponse
import es.joshluq.kmsafe.data.remote.response.RouteResponse
import es.joshluq.kmsafe.data.remote.response.SelectContractResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit service for Renting Contract and Odometer Record operations.
 */
interface RentingApiService {

    /**
     * Fetches all renting contracts for the authenticated user.
     */
    @GET("renting-contracts")
    suspend fun getContracts(): Response<RentingListResponse>

    /**
     * Registers a new renting contract.
     */
    @POST("renting-contracts")
    suspend fun createContract(
        @Body request: CreateRentingContractRequest
    ): Response<AddRentingResponse>

    /**
     * Updates an existing renting contract.
     */
    @PUT("renting-contracts/{id}")
    suspend fun updateContract(
        @Path("id") id: String,
        @Body request: UpdateRentingContractRequest
    ): Response<Unit>

    /**
     * Fetches all odometer records for a specific renting contract.
     */
    @GET("renting-contracts/{contract_id}/odometer-records")
    suspend fun getOdometerRecords(
        @Path("contract_id") contractId: String
    ): Response<OdometerRecordListResponse>

    /**
     * Registers a new odometer reading for a specific renting contract.
     */
    @POST("renting-contracts/{contract_id}/odometer-records")
    suspend fun addOdometerRecord(
        @Path("contract_id") contractId: String,
        @Body request: AddOdometerRecordRequest
    ): Response<AddOdometerRecordResponse>

    /**
     * Selects a contract as active.
     */
    @PATCH("renting-contracts/{contract_id}/select")
    suspend fun selectContract(
        @Path("contract_id") contractId: String
    ): Response<SelectContractResponse>

    /**
     * Deletes a specific renting contract.
     */
    @DELETE("renting-contracts/{contract_id}")
    suspend fun deleteContract(
        @Path("contract_id") contractId: String
    ): Response<Unit>

    /**
     * Deletes a specific odometer record.
     */
    @DELETE("odometer-records/{record_id}")
    suspend fun deleteOdometerRecord(
        @Path("record_id") recordId: String
    ): Response<Unit>

    /**
     * Updates a specific odometer record.
     */
    @PUT("odometer-records/{record_id}")
    suspend fun updateOdometerRecord(
        @Path("record_id") recordId: String,
        @Body request: UpdateOdometerRecordRequest
    ): Response<Unit>

    /**
     * Uploads the trip route for a specific odometer record.
     */
    @PUT("odometer-records/{record_id}/route")
    suspend fun uploadRoute(
        @Path("record_id") recordId: String,
        @Body request: UploadRouteRequest
    ): Response<Unit>

    /**
     * Fetches the trip route for a specific odometer record.
     */
    @GET("odometer-records/{record_id}/route")
    suspend fun getRoute(
        @Path("record_id") recordId: String
    ): Response<RouteResponse>

    /**
     * Deletes the trip route for a specific odometer record.
     */
    @DELETE("odometer-records/{record_id}/route")
    suspend fun deleteRoute(
        @Path("record_id") recordId: String
    ): Response<Unit>
}
