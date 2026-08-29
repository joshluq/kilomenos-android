package es.joshluq.kmsafe.domain.service

/**
 * Interface for providing a unique device fingerprint.
 */
interface FingerprintProvider {
    /**
     * Returns a stable identifier for the device.
     */
    fun getFingerprint(): String
}
