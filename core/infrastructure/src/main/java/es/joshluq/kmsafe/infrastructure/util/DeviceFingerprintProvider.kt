package es.joshluq.kmsafe.infrastructure.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides a stable, hardware-tied fingerprint of the device.
 */
@Singleton
class DeviceFingerprintProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Returns SHA-256 hash of the Android ID.
     */
    @SuppressLint("HardwareIds")
    fun getFingerprint(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return androidId?.hashSha256() ?: "unknown_device"
    }

    private fun String.hashSha256(): String {
        val bytes = this.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
