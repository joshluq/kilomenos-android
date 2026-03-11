package es.joshluq.kmsafe.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.SyncStatus

/**
 * Room Entity representing an odometer record in the local database.
 */
@Entity(tableName = "odometer_record")
data class OdometerRecordEntity(
    @PrimaryKey val id: String,
    val contractId: String,
    val timestamp: Long,
    val odometerValue: Int,
    val isInitialRecord: Boolean,
    val label: String? = null,
    val fuelAmount: Double? = null,
    val syncStatus: String = "SYNCED"
)

/**
 * Extension function to map Entity to Domain model.
 */
fun OdometerRecordEntity.toDomain(): OdometerRecord = OdometerRecord(
    id = id,
    contractId = contractId,
    timestamp = timestamp,
    odometerValue = odometerValue,
    isInitialRecord = isInitialRecord,
    label = label,
    fuelAmount = fuelAmount,
    syncStatus = SyncStatus.valueOf(syncStatus)
)

/**
 * Extension function to map Domain model to Entity.
 */
fun OdometerRecord.toEntity(): OdometerRecordEntity = OdometerRecordEntity(
    id = id,
    contractId = contractId,
    timestamp = timestamp,
    odometerValue = odometerValue,
    isInitialRecord = isInitialRecord,
    label = label,
    fuelAmount = fuelAmount,
    syncStatus = syncStatus.name
)
