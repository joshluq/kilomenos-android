package es.joshluq.kmsafe.infrastructure.remote.api

import es.joshluq.kmsafe.infrastructure.remote.request.OAuthSignInRequest
import es.joshluq.kmsafe.infrastructure.remote.request.RefreshRequest
import es.joshluq.kmsafe.infrastructure.remote.request.SignInRequest
import es.joshluq.kmsafe.infrastructure.remote.request.SignUpRequest
import es.joshluq.kmsafe.infrastructure.remote.response.OAuthSignInResponse
import es.joshluq.kmsafe.infrastructure.remote.response.RefreshResponse
import es.joshluq.kmsafe.infrastructure.remote.response.SignInResponse
import es.joshluq.kmsafe.infrastructure.remote.response.SignUpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service for public authentication operations.
 */
interface AuthApiService {

    /**
     * Authenticates a user.
     */
    @POST("auth/signin")
    suspend fun signIn(
        @Body request: SignInRequest
    ): Response<SignInResponse>

    /**
     * Registers a new user.
     */
    @POST("auth/signup")
    suspend fun signUp(
        @Body request: SignUpRequest
    ): Response<SignUpResponse>

    /**
     * Refreshes the authentication session.
     */
    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshRequest
    ): Response<RefreshResponse>

    /**
     * Authenticates a user using OAuth.
     */
    @POST("auth/oauth")
    suspend fun signInWithOAuth(
        @Body request: OAuthSignInRequest
    ): Response<OAuthSignInResponse>
}
