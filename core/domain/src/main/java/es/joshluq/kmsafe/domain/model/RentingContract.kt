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
    val totalKms: Double,
    val startOdometer: Double,
    val currentOdometer: Double,
    val isSelected: Boolean = false,
    val vehicleImageUrl: String? = null,
    val bluetoothDeviceName: String? = null,
    val bluetoothDeviceAddress: String? = null,
    val excessDistancePrice: Double? = null,
    val courtesyMarginKms: Double = 0.0,
    val fuelType: FuelType = FuelType.GASOLINE_95,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
) {
    companion object {
        /**
         * Benchmark market rate in Spain/Europe for standard passenger car excess km penalties (0.06 €/km).
         */
        const val DEFAULT_MARKET_EXCESS_PRICE = 0.06f

        /**
         * Benchmark courtesy margin or tolerance buffer offered in renting/leasing contracts (500.0 km).
         */
        const val DEFAULT_MARKET_COURTESY_MARGIN_KMS = 500.0
    }
}

