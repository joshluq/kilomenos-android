package es.joshluq.kmsafe.infrastructure.notifications

import com.google.firebase.messaging.FirebaseMessaging
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.service.DeviceTokenProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Concrete implementation of [DeviceTokenProvider] resolving tokens from Firebase Cloud Messaging.
 *
 * NOTE ON DEPRECATION:
 * In Firebase Android SDK (BOM 34+ / firebase-messaging 25.1+), Google marked [FirebaseMessaging.getToken]
 * as deprecated in favor of [FirebaseMessaging.register] as part of the ecosystem migration toward
 * Firebase Installation IDs (FID).
 *
 * However, our backend API (Hono / Supabase table `user_fcm_tokens` and FCM HTTP v1 dispatchers) strictly
 * requires the registration token (`message.token`), NOT the FID handle. Using the new `register()` API
 * would deliver an FID which cannot be used with current backend FCM HTTP v1 token endpoints.
 * Therefore, [FirebaseMessaging.getToken] is intentionally retained and suppressed here.
 */
@Singleton
class FirebaseDeviceTokenProvider @Inject constructor(
    private val logger: LoggerKit
) : DeviceTokenProvider {

    @Suppress("DEPRECATION")
    override suspend fun getDeviceToken(): String? = suspendCancellableCoroutine { continuation ->
        try {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        logger.d("FirebaseDeviceTokenProvider", "FCM token resolved successfully")
                        continuation.resume(token)
                    } else {
                        val error = task.exception
                        logger.w("FirebaseDeviceTokenProvider", "Failed to resolve FCM token: ${error?.message}")
                        continuation.resume(null)
                    }
                }
        } catch (e: Exception) {
            logger.e("FirebaseDeviceTokenProvider", "Exception while fetching FCM token: ${e.message}", e)
            continuation.resume(null)
        }
    }
}
