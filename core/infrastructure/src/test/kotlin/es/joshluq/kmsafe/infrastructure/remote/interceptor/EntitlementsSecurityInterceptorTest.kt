package es.joshluq.kmsafe.infrastructure.remote.interceptor

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.infrastructure.remote.auth.UserSessionDataSource
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Provider

class EntitlementsSecurityInterceptorTest {

    private val sessionDataSource: UserSessionDataSource = mockk(relaxed = true)
    private val logger: LoggerKit = mockk(relaxed = true)
    private val sessionProvider: Provider<UserSessionDataSource> = Provider { sessionDataSource }
    private lateinit var interceptor: EntitlementsSecurityInterceptor
    private val chain: Interceptor.Chain = mockk()

    private val testRequest = Request.Builder()
        .url("https://api.kmsafe.es/v1/user/entitlements")
        .build()

    @Before
    fun setUp() {
        interceptor = EntitlementsSecurityInterceptor(sessionProvider, logger)
        interceptor.securityScope = CoroutineScope(Dispatchers.Unconfined)
        every { chain.request() } returns testRequest
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given 200 OK response when intercept then does not downgrade session`() {
        val response = Response.Builder()
            .request(testRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()

        every { chain.proceed(testRequest) } returns response

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
        coVerify(exactly = 0) { sessionDataSource.downgradeToFree() }
    }

    @Test
    fun `given 403 response with PREMIUM_REQUIRED in body when intercept then downgrades session to FREE`() {
        val errorJson = """{"error": "PREMIUM_REQUIRED", "message": "Subscription expired"}"""
        val response = Response.Builder()
            .request(testRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(403)
            .message("Forbidden")
            .body(errorJson.toResponseBody("application/json".toMediaType()))
            .build()

        every { chain.proceed(testRequest) } returns response
        coEvery { sessionDataSource.downgradeToFree() } returns Unit

        val result = interceptor.intercept(chain)

        assertEquals(403, result.code)
        coVerify(exactly = 1) { sessionDataSource.downgradeToFree() }
    }

    @Test
    fun `given 403 response with PREMIUM_REQUIRED header when intercept then downgrades session to FREE`() {
        val response = Response.Builder()
            .request(testRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(403)
            .message("Forbidden")
            .header("X-Error-Code", "PREMIUM_REQUIRED")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()

        every { chain.proceed(testRequest) } returns response
        coEvery { sessionDataSource.downgradeToFree() } returns Unit

        val result = interceptor.intercept(chain)

        assertEquals(403, result.code)
        coVerify(exactly = 1) { sessionDataSource.downgradeToFree() }
    }

    @Test
    fun `given 403 response without PREMIUM_REQUIRED when intercept then does not downgrade session`() {
        val errorJson = """{"error": "INVALID_TOKEN", "message": "Signature invalid"}"""
        val response = Response.Builder()
            .request(testRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(403)
            .message("Forbidden")
            .body(errorJson.toResponseBody("application/json".toMediaType()))
            .build()

        every { chain.proceed(testRequest) } returns response

        val result = interceptor.intercept(chain)

        assertEquals(403, result.code)
        coVerify(exactly = 0) { sessionDataSource.downgradeToFree() }
    }
}
