package es.joshluq.kmsafe.infrastructure.remote.interceptor

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.infrastructure.remote.auth.UserSessionDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * OkHttp Interceptor enforcing the Zero-Trust security layer for entitlements.
 * When any protected API responds with HTTP 403 Forbidden containing "PREMIUM_REQUIRED",
 * this interceptor asynchronously degrades the local session state to FREE without blocking the network thread.
 */
@Singleton
class EntitlementsSecurityInterceptor @Inject constructor(
    private val sessionDataSourceProvider: Provider<UserSessionDataSource>,
    private val logger: LoggerKit
) : Interceptor {

    // Dedicated background scope for security side-effects without blocking OkHttp dispatchers
    internal var securityScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val HEADER_ERROR_CODE = "X-Error-Code"
        const val CODE_PREMIUM_REQUIRED = "PREMIUM_REQUIRED"
        private const val PEEK_BODY_MAX_BYTES = 2048L
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 403) {
            val hasPremiumHeader = response.header(HEADER_ERROR_CODE)
                ?.contains(CODE_PREMIUM_REQUIRED, ignoreCase = true) == true

            // Lazy evaluation: only peek buffer if header is not present to avoid unnecessary allocations
            val isSecurityViolation = hasPremiumHeader || checkBodyForSecurityViolation(response)

            if (isSecurityViolation) {
                logger.w(
                    "EntitlementsSecurityInterceptor",
                    "HTTP 403 with $CODE_PREMIUM_REQUIRED detected on ${request.url}. Enforcing async local downgrade to FREE."
                )
                securityScope.launch {
                    runCatching {
                        sessionDataSourceProvider.get().downgradeToFree()
                    }.onFailure { e ->
                        logger.e("EntitlementsSecurityInterceptor", "Failed to downgrade session: ${e.message}", e)
                    }
                }
            }
        }

        return response
    }

    private fun checkBodyForSecurityViolation(response: Response): Boolean {
        return runCatching {
            response.peekBody(PEEK_BODY_MAX_BYTES).string().contains(CODE_PREMIUM_REQUIRED, ignoreCase = true)
        }.getOrDefault(false)
    }
}
