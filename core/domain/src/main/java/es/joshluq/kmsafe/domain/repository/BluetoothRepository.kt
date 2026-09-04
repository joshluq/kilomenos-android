package es.joshluq.kmsafe.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Domain repository for inspecting and observing Bluetooth device connection states.
 * Adheres to Domain Purity with zero platform framework dependencies.
 */
interface BluetoothRepository {

    /**
     * Observes whether a device with the given MAC address is currently connected.
     *
     * @param macAddress Bluetooth hardware address (e.g. "AA:BB:CC:DD:EE:FF").
     * @return Flow emitting true when the device is actively connected, false otherwise.
     */
    fun observeDeviceConnection(macAddress: String): Flow<Boolean>

    /**
     * Checks synchronously whether a device with the given MAC address is currently connected.
     *
     * @param macAddress Bluetooth hardware address.
     * @return True if currently connected, false otherwise.
     */
    suspend fun isDeviceConnected(macAddress: String): Boolean
}
