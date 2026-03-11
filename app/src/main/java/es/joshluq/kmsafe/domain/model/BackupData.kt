package es.joshluq.kmsafe.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain model representing the full backup of the application data.
 *
 * @property contracts The list of all renting contracts.
 * @property history The list of all odometer records.
 */
@Serializable
data class BackupData(
    val contracts: List<RentingContract> = emptyList(),
    val history: List<OdometerRecord> = emptyList()
)
