package es.joshluq.kmsafe.core.ui.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

/**
 * Utility functions for safely querying Bluetooth devices and connection states.
 */
object BluetoothUtils {

    /**
     * Checks whether the application has runtime permission to connect and inspect Bluetooth devices.
     * Required on Android 12+ (API 31+).
     *
     * @param context Android context.
     * @return True if permission is granted or not required.
     */
    fun hasBluetoothConnectPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Normalizes a Bluetooth MAC address for uniform comparisons (strips colons, uppercases, trims).
     *
     * @param address Raw Bluetooth MAC address.
     * @return Normalized MAC address string.
     */
    fun normalizeAddress(address: String): String {
        return address.replace(":", "").uppercase().trim()
    }

    /**
     * Asynchronously retrieves the set of normalized MAC addresses of devices currently connected
     * via audio (A2DP) or hands-free (HEADSET) profiles.
     *
     * @param context Android context.
     * @param timeoutMillis Maximum time to wait for BluetoothProfile proxies before returning.
     * @return Set of normalized MAC addresses currently connected.
     */
    @SuppressLint("MissingPermission")
    suspend fun getConnectedAudioDeviceAddresses(
        context: Context,
        timeoutMillis: Long = 2000L
    ): Set<String> {
        if (!hasBluetoothConnectPermission(context)) return emptySet()

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return emptySet()
        val adapter: BluetoothAdapter = bluetoothManager.adapter ?: return emptySet()
        if (!adapter.isEnabled) return emptySet()

        val connectedMacs = Collections.synchronizedSet(mutableSetOf<String>())

        withTimeoutOrNull(timeoutMillis.milliseconds) {
            suspendCancellableCoroutine { continuation ->
                var a2dpProxy: BluetoothProfile? = null
                var headsetProxy: BluetoothProfile? = null
                var a2dpProcessed = false
                var headsetProcessed = false

                fun checkCompletion() {
                    if (a2dpProcessed && headsetProcessed && !continuation.isCompleted) {
                        continuation.resume(Unit)
                    }
                }

                val profileListener = object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        try {
                            if (profile == BluetoothProfile.A2DP) {
                                a2dpProxy = proxy
                                proxy.connectedDevices.forEach { device ->
                                    connectedMacs.add(normalizeAddress(device.address))
                                }
                                a2dpProcessed = true
                            } else if (profile == BluetoothProfile.HEADSET) {
                                headsetProxy = proxy
                                proxy.connectedDevices.forEach { device ->
                                    connectedMacs.add(normalizeAddress(device.address))
                                }
                                headsetProcessed = true
                            }
                        } catch (_: SecurityException) {
                            // Permission revoked or not accessible
                        } catch (_: Exception) {
                            // Safe fallback
                        } finally {
                            checkCompletion()
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {
                        if (profile == BluetoothProfile.A2DP) a2dpProcessed = true
                        if (profile == BluetoothProfile.HEADSET) headsetProcessed = true
                        checkCompletion()
                    }
                }

                try {
                    adapter.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)
                    adapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
                } catch (_: Exception) {
                    if (!continuation.isCompleted) continuation.resume(Unit)
                }

                continuation.invokeOnCancellation {
                    try {
                        a2dpProxy?.let { adapter.closeProfileProxy(BluetoothProfile.A2DP, it) }
                        headsetProxy?.let { adapter.closeProfileProxy(BluetoothProfile.HEADSET, it) }
                    } catch (_: Exception) {
                        // Cleanup silently
                    }
                }
            }
        }

        return connectedMacs.toSet()
    }

    /**
     * Safely retrieves all bonded (paired) Bluetooth devices.
     *
     * @param context Android context.
     * @return List of bonded BluetoothDevice instances or empty list.
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(context: Context): List<BluetoothDevice> {
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        val adapter: BluetoothAdapter? = bluetoothManager?.adapter
        return adapter?.bondedDevices?.toList() ?: emptyList()
    }

}
