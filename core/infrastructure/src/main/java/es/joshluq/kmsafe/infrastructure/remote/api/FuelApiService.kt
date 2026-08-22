package es.joshluq.kmsafe.infrastructure.remote.api

import es.joshluq.kmsafe.infrastructure.remote.request.FuelExpenseRequest
import es.joshluq.kmsafe.infrastructure.remote.request.StationRequest
import es.joshluq.kmsafe.infrastructure.remote.response.FuelExpenseResponse
import es.joshluq.kmsafe.infrastructure.remote.response.StationListResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit service for Fuel Expenses and Service Stations.
 */
interface FuelApiService {

    // --- Stations ---

    /**
     * Fetches all service stations for the authenticated user.
     */
    @GET("stations")
    suspend fun getStations(): Response<StationListResponse>

    /**
     * Synchronizes a batch of service stations (Upsert).
     */
    @POST("stations/sync")
    suspend fun syncStations(
        @Body stations: List<StationRequest>
    ): Response<StationListResponse>

    /**
     * Deletes a specific service station.
     */
    @DELETE("stations/{id}")
    suspend fun deleteStation(
        @Path("id") id: String
    ): Response<Unit>

    // --- Fuel Expenses ---

    /**
     * Fetches all fuel expenses for a specific contract.
     */
    @GET("renting-contracts/{contract_id}/fuel-expenses")
    suspend fun getFuelExpenses(
        @Path("contract_id") contractId: String
    ): Response<es.joshluq.kmsafe.infrastructure.remote.response.FuelExpenseListResponse>

    /**
     * Registers a new fuel expense or price report for a specific contract.
     */
    @POST("renting-contracts/{contract_id}/fuel-expenses")
    suspend fun createFuelExpense(
        @Path("contract_id") contractId: String,
        @Body request: FuelExpenseRequest
    ): Response<FuelExpenseResponse>

    /**
     * Updates an existing fuel expense record.
     */
    @PUT("fuel-expenses/{id}")
    suspend fun updateFuelExpense(
        @Path("id") id: String,
        @Body request: FuelExpenseRequest
    ): Response<FuelExpenseResponse>

    /**
     * Deletes a specific fuel expense.
     */
    @DELETE("fuel-expenses/{id}")
    suspend fun deleteFuelExpense(
        @Path("id") id: String
    ): Response<Unit>
}
