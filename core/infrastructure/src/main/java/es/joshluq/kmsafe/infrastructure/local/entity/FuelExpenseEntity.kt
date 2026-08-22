package es.joshluq.kmsafe.infrastructure.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.SyncStatus

/**
 * Room Entity representing a fuel or EV charging expense in the local database.
 */
@Entity(
    tableName = "fuel_expenses",
    foreignKeys = [
        ForeignKey(
            entity = RentingContractEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("vehicleId"),
        Index("stationId"),
        Index("timestamp")
    ]
)
data class FuelExpenseEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val stationId: String? = null,
    val stationName: String? = null,
    val timestamp: Long,
    val fuelType: String,
    val unitPrice: Double,
    val volumeQuantity: Double,
    val totalCost: Double,
    val odometerAtExpense: Double? = null,
    val isFullTank: Boolean = true,
    val notes: String? = null,
    val kmSinceLastRefuel: Double? = null,
    val consumptionPer100km: Double? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED"
)

/**
 * Maps Entity to Domain model.
 */
fun FuelExpenseEntity.toDomain(): FuelExpense = FuelExpense(
    id = id,
    vehicleId = vehicleId,
    stationId = stationId,
    stationName = stationName,
    timestamp = timestamp,
    fuelType = FuelType.fromName(fuelType),
    unitPrice = unitPrice,
    volumeQuantity = volumeQuantity,
    totalCost = totalCost,
    odometerAtExpense = odometerAtExpense,
    isFullTank = isFullTank,
    notes = notes,
    kmSinceLastRefuel = kmSinceLastRefuel,
    consumptionPer100km = consumptionPer100km,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.valueOf(syncStatus)
)

/**
 * Maps Domain model to Entity.
 */
fun FuelExpense.toEntity(): FuelExpenseEntity = FuelExpenseEntity(
    id = id,
    vehicleId = vehicleId,
    stationId = stationId,
    stationName = stationName,
    timestamp = timestamp,
    fuelType = fuelType.name,
    unitPrice = unitPrice,
    volumeQuantity = volumeQuantity,
    totalCost = totalCost,
    odometerAtExpense = odometerAtExpense,
    isFullTank = isFullTank,
    notes = notes,
    kmSinceLastRefuel = kmSinceLastRefuel,
    consumptionPer100km = consumptionPer100km,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name
)
