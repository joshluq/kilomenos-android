package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.repository.NotificationRepository
import es.joshluq.kmsafe.domain.service.DeviceTokenProvider
import es.joshluq.kmsafe.domain.service.FingerprintProvider
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RegisterDeviceTokenUseCaseTest {

    private val notificationRepository: NotificationRepository = mockk(relaxed = true)
    private val deviceTokenProvider: DeviceTokenProvider = mockk(relaxed = true)
    private val fingerprintProvider: FingerprintProvider = mockk(relaxed = true)

    private lateinit var useCase: RegisterDeviceTokenUseCase

    @Before
    fun setUp() {
        useCase = RegisterDeviceTokenUseCaseImpl(
            notificationRepository = notificationRepository,
            deviceTokenProvider = deviceTokenProvider,
            fingerprintProvider = fingerprintProvider
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given explicit token when invoked then registers token with device fingerprint`() = runTest {
        val token = "explicit_fcm_token_123"
        val deviceId = "hw_fingerprint_abc"
        coEvery { fingerprintProvider.getFingerprint() } returns deviceId
        coEvery { notificationRepository.registerDeviceToken(token, deviceId) } returns Result.success(Unit)

        val result = useCase(RegisterDeviceTokenUseCase.Input(token))

        assertTrue(result.isSuccess)
        assertEquals(RegisterDeviceTokenUseCase.Output.Success, result.getOrNull())
        coVerify(exactly = 1) {
            notificationRepository.registerDeviceToken(token, deviceId)
        }
        coVerify(exactly = 0) {
            deviceTokenProvider.getDeviceToken()
        }
    }

    @Test
    fun `given null token when invoked then resolves from provider and registers`() = runTest {
        val resolvedToken = "resolved_fcm_token_456"
        val deviceId = "hw_fingerprint_abc"
        coEvery { deviceTokenProvider.getDeviceToken() } returns resolvedToken
        coEvery { fingerprintProvider.getFingerprint() } returns deviceId
        coEvery { notificationRepository.registerDeviceToken(resolvedToken, deviceId) } returns Result.success(Unit)

        val result = useCase(RegisterDeviceTokenUseCase.Input())

        assertTrue(result.isSuccess)
        assertEquals(RegisterDeviceTokenUseCase.Output.Success, result.getOrNull())
        coVerify(exactly = 1) {
            deviceTokenProvider.getDeviceToken()
            notificationRepository.registerDeviceToken(resolvedToken, deviceId)
        }
    }

    @Test
    fun `given blank token when invoked then returns failure without calling repository`() = runTest {
        coEvery { deviceTokenProvider.getDeviceToken() } returns "  "

        val result = useCase(RegisterDeviceTokenUseCase.Input())

        assertTrue(result.isFailure)
        assertEquals("FCM Token is null or blank", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) {
            notificationRepository.registerDeviceToken(any(), any())
        }
    }
}
