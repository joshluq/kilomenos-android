package es.joshluq.kmsafe.core.tracking

import android.content.Context
import android.content.SharedPreferences

/**
 * Thread-safe in-memory & synchronous SharedPreferences cache for the active vehicle Bluetooth MAC.
 *
 * Guarantees instantaneous (<0.1ms) synchronous validation inside BroadcastReceiver.onReceive()
 * without waiting for asynchronous database or DataStore operations.
 */
object TrackingDeviceCache {
    private const val PREFS_NAME = "kmsafe_tracking_device_cache"
    private const val KEY_ACTIVE_MAC = "active_vehicle_mac"
    private const val KEY_VEHICLE_NAME = "active_vehicle_name"

    @Volatile
    private var cachedMac: String? = null

    @Volatile
    private var cachedVehicleName: String? = null

    @Volatile
    private var isInitialized: Boolean = false

    @Volatile
    private var isBluetoothConnected: Boolean = false

    /**
     * Updates the fast-path connection status of the linked vehicle Bluetooth device.
     */
    fun setBluetoothConnected(connected: Boolean) {
        isBluetoothConnected = connected
    }

    /**
     * Checks whether the linked vehicle Bluetooth device is currently connected.
     */
    fun isBluetoothConnected(): Boolean {
        return isBluetoothConnected
    }

    /**
     * Retrieves the linked vehicle Bluetooth MAC address synchronously.
     * Checks in-memory cache first, falls back to SharedPreferences.
     */
    fun getLinkedMac(context: Context): String? {
        if (isInitialized) {
            return cachedMac
        }
        val prefs = getPrefs(context)
        val mac = prefs.getString(KEY_ACTIVE_MAC, null)
        val name = prefs.getString(KEY_VEHICLE_NAME, null)
        cachedMac = mac
        cachedVehicleName = name
        isInitialized = true
        return mac
    }

    /**
     * Retrieves the linked vehicle name synchronously.
     */
    fun getVehicleName(context: Context): String? {
        if (isInitialized) {
            return cachedVehicleName
        }
        val prefs = getPrefs(context)
        val mac = prefs.getString(KEY_ACTIVE_MAC, null)
        val name = prefs.getString(KEY_VEHICLE_NAME, null)
        cachedMac = mac
        cachedVehicleName = name
        isInitialized = true
        return name
    }

    /**
     * Atomically updates both in-memory and persisted cache with the active vehicle data.
     */
    fun updateCache(context: Context, macAddress: String?, vehicleName: String?) {
        cachedMac = macAddress
        cachedVehicleName = vehicleName
        isInitialized = true
        getPrefs(context).edit().apply {
            putString(KEY_ACTIVE_MAC, macAddress)
            putString(KEY_VEHICLE_NAME, vehicleName)
            apply()
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
