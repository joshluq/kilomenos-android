package es.joshluq.kmsafe.data.remote.api

import es.joshluq.kmsafe.data.remote.request.UpdateSubscriptionRequest
import es.joshluq.kmsafe.data.remote.response.UpdateSubscriptionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PUT

/**
 * Retrofit service for authentication operations that require a valid JWT token.
 */
interface AuthenticatedAuthApiService {

    /**
     * Updates the user's subscription level.
     */
    @PUT("auth/subscription")
    suspend fun updateSubscription(
        @Body request: UpdateSubscriptionRequest
    ): Response<UpdateSubscriptionResponse>

    /**
     * Permanently deletes the user account and all associated data.
     */
    @DELETE("auth/delete-account")
    suspend fun deleteAccount(): Response<Unit>
}
