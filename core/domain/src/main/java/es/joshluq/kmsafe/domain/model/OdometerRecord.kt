package es.joshluq.kmsafe.domain.model

import kotlinx.serialization.Serializable

/**
 * Immutable domain model representing a single odometer reading entry.
 */
@Serializable
data class OdometerRecord(
    val id: String,
    val contractId: String = "",
    val timestamp: Long,
    val odometerValue: Double,
    val isInitialRecord: Boolean,
    val label: String? = null,
    val fuelAmount: Double? = null,
    val hasRoute: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
