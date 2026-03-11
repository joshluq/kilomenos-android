package es.joshluq.kmsafe.data.remote.auth

import es.joshluq.authkit.network.sdk.TokenRefresher
import es.joshluq.authkit.session.model.Token
import es.joshluq.authkit.session.model.TokenHolder
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.data.remote.api.AuthApiService
import es.joshluq.kmsafe.data.remote.request.RefreshRequest
import javax.inject.Inject

/**
 * Implementation of [TokenRefresher] that communicates with the Auth API to renew tokens.
 */
class AuthTokenRefresher @Inject constructor(
    private val apiService: AuthApiService,
    private val logger: LoggerKit
) : TokenRefresher {

    override suspend fun refresh(oldTokens: TokenHolder): Result<TokenHolder> {
        logger.d("AuthTokenRefresher", "Attempting to refresh token")
        val refreshToken = oldTokens.getRefreshToken()?.value ?: return Result.failure(
            Exception("No refresh token available")
        )

        return runCatching {
            val response = apiService.refreshToken(RefreshRequest(refreshToken))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.session != null) {
                    logger.i("AuthTokenRefresher", "Token refreshed successfully")
                    TokenHolder.withTokens(
                        Token.Access(body.session.accessToken ?: ""),
                        Token.Refresh(body.session.refreshToken ?: "")
                    )
                } else {
                    val error = body?.error ?: "Refresh failed"
                    logger.e("AuthTokenRefresher", "Refresh failed with error: $error")
                    throw Exception(error)
                }
            } else {
                logger.e("AuthTokenRefresher", "Refresh failed with HTTP error: ${response.code()}")
                throw Exception("Refresh unauthorized")
            }
        }
    }
}
