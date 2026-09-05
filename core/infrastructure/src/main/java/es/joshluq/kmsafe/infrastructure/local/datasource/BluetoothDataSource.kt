package es.joshluq.kmsafe.infrastructure.local.datasource

import es.joshluq.foundationkit.log.LoggerKit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory reactive data source for Bluetooth hardware connection events.
 * Bridges synchronous broadcast receivers with active observers.
 */
@Singleton
class BluetoothDataSource @Inject constructor(
    private val logger: LoggerKit
) {
    private val _connectionUpdates = MutableSharedFlow<Pair<String, Boolean>>(replay = 1)

    fun observeDeviceUpdates(targetMac: String): Flow<Boolean> {
        val normalized = normalizeAddress(targetMac)
        return _connectionUpdates
            .filter { (mac, _) -> normalizeAddress(mac) == normalized }
            .map { (_, isConnected) -> isConnected }
    }

    fun emitConnectionUpdate(macAddress: String, isConnected: Boolean) {
        val normalized = normalizeAddress(macAddress)
        logger.d("BluetoothDataSource", "Emitting connection update for $normalized: $isConnected")
        _connectionUpdates.tryEmit(normalized to isConnected)
    }

    private fun normalizeAddress(address: String): String {
        return address.replace(":", "").replace("-", "").uppercase().trim()
    }
}
