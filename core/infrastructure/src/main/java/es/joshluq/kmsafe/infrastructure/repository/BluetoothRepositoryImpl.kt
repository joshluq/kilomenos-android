package es.joshluq.kmsafe.infrastructure.repository

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.repository.BluetoothRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

/**
 * Android infrastructure implementation of [BluetoothRepository].
 * Observes Bluetooth hardware states using reactive callback flows and profile proxies.
 */
@Singleton
class BluetoothRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: LoggerKit
) : BluetoothRepository {

    override fun observeDeviceConnection(macAddress: String): Flow<Boolean> = callbackFlow {
        val normalizedTarget = normalizeAddress(macAddress)
        logger.d("BluetoothRepository", "Starting observation for target MAC: $normalizedTarget")

        // 1. Initial asynchronous check
        launch {
            val initiallyConnected = isDeviceConnected(macAddress)
            trySend(initiallyConnected)
        }

        // 2. BroadcastReceiver for hardware ACL events
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val action = intent?.action ?: return
                val device = intent.getBluetoothDevice() ?: return
                val deviceAddress = normalizeAddress(device.address ?: "")

                if (deviceAddress == normalizedTarget) {
                    when (action) {
                        BluetoothDevice.ACTION_ACL_CONNECTED -> {
                            logger.i("BluetoothRepository", "Device $deviceAddress CONNECTED")
                            trySend(true)
                        }
                        BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                            logger.i("BluetoothRepository", "Device $deviceAddress DISCONNECTED")
                            trySend(false)
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        awaitClose {
            logger.d("BluetoothRepository", "Closing observation for target MAC: $normalizedTarget")
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
                // Receiver was already unregistered
            }
        }
    }

    override suspend fun isDeviceConnected(macAddress: String): Boolean {
        if (macAddress.isBlank()) return false
        val normalizedTarget = normalizeAddress(macAddress)
        val connected = getConnectedAudioDeviceAddresses()
        return connected.contains(normalizedTarget)
    }

    private fun normalizeAddress(address: String): String {
        return address.replace(":", "").uppercase().trim()
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getConnectedAudioDeviceAddresses(timeoutMillis: Long = 1200L): Set<String> {
        if (!hasBluetoothConnectPermission()) return emptySet()

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
                            // Permission revoked
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

    private fun Intent.getBluetoothDevice(): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }
}
