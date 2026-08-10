package es.joshluq.kmsafe.domain.model

import kotlinx.serialization.Serializable

/**
 * Immutable domain model representing the initial renting configuration.
 */
@Serializable
data class RentingContract(
    val id: String = "",
    val userId: String = "",
    val vehicleName: String,
    val startDate: Long,
    val durationMonths: Int,
    val totalKms: Int,
    val startOdometer: Int,
    val currentOdometer: Int,
    val isSelected: Boolean = false,
    val vehicleImageUrl: String? = null,
    val bluetoothDeviceAddress: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
