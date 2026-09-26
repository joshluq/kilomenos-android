package es.joshluq.kmsafe.domain.service

/**
 * Interface to provide the device push notification token (e.g. FCM token)
 * without coupling the domain layer to platform-specific SDKs.
 */
interface DeviceTokenProvider {
    /**
     * Resolves the current push token for the device, or null if unavailable.
     */
    suspend fun getDeviceToken(): String?
}
