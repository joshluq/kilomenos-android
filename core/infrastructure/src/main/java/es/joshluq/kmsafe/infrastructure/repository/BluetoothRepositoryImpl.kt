package es.joshluq.kmsafe.infrastructure.repository

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
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
import es.joshluq.kmsafe.infrastructure.local.datasource.BluetoothDataSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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
 * Observes Bluetooth hardware states using reactive flows, [BluetoothDataSource] bridge,
 * broadcast receivers, and profile proxies.
 */
@Singleton
class BluetoothRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothDataSource: BluetoothDataSource,
    private val logger: LoggerKit
) : BluetoothRepository {

    override fun updateDeviceConnectionState(macAddress: String, isConnected: Boolean) {
        bluetoothDataSource.emitConnectionUpdate(macAddress, isConnected)
    }

    override fun observeDeviceConnection(macAddress: String): Flow<Boolean> = callbackFlow {
        val normalizedTarget = normalizeAddress(macAddress)
        logger.d("BluetoothRepository", "Starting observation for target MAC: $normalizedTarget")

        // 1. Initial asynchronous check of current connected audio devices
        launch {
            val initiallyConnected = isDeviceConnected(macAddress)
            logger.d("BluetoothRepository", "Initial connection check for $normalizedTarget: $initiallyConnected")
            trySend(initiallyConnected)
        }

        // 2. Observe events from BluetoothDataSource (fed by BluetoothConnectionReceiver)
        val dataSourceJob = launch {
            bluetoothDataSource.observeDeviceUpdates(macAddress).collect { isConnected ->
                logger.d("BluetoothRepository", "DataSource update for $normalizedTarget: $isConnected")
                trySend(isConnected)
            }
        }

        // 3. BroadcastReceiver for hardware ACL events and Bluetooth adapter/profile changes
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val action = intent?.action ?: return
                logger.d("BluetoothRepository", "Received broadcast: $action")
                when (action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val device = intent.getBluetoothDevice()
                        val deviceAddress = try {
                            normalizeAddress(device?.address ?: "")
                        } catch (_: SecurityException) {
                            ""
                        }
                        if (deviceAddress == normalizedTarget) {
                            logger.i("BluetoothRepository", "Device $deviceAddress CONNECTED via dynamic receiver")
                            trySend(true)
                        }
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED,
                    BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> {
                        val device = intent.getBluetoothDevice()
                        val deviceAddress = try {
                            normalizeAddress(device?.address ?: "")
                        } catch (_: SecurityException) {
                            ""
                        }
                        if ((deviceAddress == normalizedTarget) || deviceAddress.isEmpty()) {
                            logger.i("BluetoothRepository", "Device $deviceAddress DISCONNECTED via dynamic receiver")
                            trySend(false)
                        } else {
                            launch {
                                val stillConnected = isDeviceConnected(macAddress)
                                trySend(stillConnected)
                            }
                        }
                    }
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                        launch {
                            val isConnected = isDeviceConnected(macAddress)
                            trySend(isConnected)
                        }
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                            logger.i("BluetoothRepository", "Bluetooth adapter turned OFF")
                            trySend(false)
                        } else if (state == BluetoothAdapter.STATE_ON) {
                            launch {
                                val isConnected = isDeviceConnected(macAddress)
                                trySend(isConnected)
                            }
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }

        try {
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            logger.e("BluetoothRepository", "Error registering receiver", e)
        }

        awaitClose {
            logger.d("BluetoothRepository", "Closing observation for target MAC: $normalizedTarget")
            dataSourceJob.cancel()
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
                // Receiver was already unregistered
            }
        }
    }.distinctUntilChanged()

    override suspend fun isDeviceConnected(macAddress: String): Boolean {
        if (macAddress.isBlank()) return false
        val normalizedTarget = normalizeAddress(macAddress)
        val connected = getConnectedAudioDeviceAddresses()
        return connected.contains(normalizedTarget)
    }

    private fun normalizeAddress(address: String): String {
        return address.replace(":", "").replace("-", "").uppercase().trim()
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
                                    try {
                                        connectedMacs.add(normalizeAddress(device.address))
                                    } catch (_: SecurityException) {
                                    }
                                }
                                a2dpProcessed = true
                            } else if (profile == BluetoothProfile.HEADSET) {
                                headsetProxy = proxy
                                proxy.connectedDevices.forEach { device ->
                                    try {
                                        connectedMacs.add(normalizeAddress(device.address))
                                    } catch (_: SecurityException) {
                                    }
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
                    val a2dpStarted = adapter.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)
                    if (!a2dpStarted) a2dpProcessed = true

                    val headsetStarted = adapter.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
                    if (!headsetStarted) headsetProcessed = true

                    checkCompletion()
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
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    ?: @Suppress("DEPRECATION") getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            } else {
                @Suppress("DEPRECATION")
                getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }
        } catch (_: Exception) {
            @Suppress("DEPRECATION")
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }
}
