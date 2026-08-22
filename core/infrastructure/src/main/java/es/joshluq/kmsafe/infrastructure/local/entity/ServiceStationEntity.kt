package es.joshluq.kmsafe.infrastructure.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.domain.model.SyncStatus

/**
 * Room Entity representing a service station or EV charging point.
 */
@Entity(
    tableName = "service_stations",
    indices = [
        Index("isFavorite"),
        Index(value = ["latitude", "longitude"])
    ]
)
data class ServiceStationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val isFavorite: Boolean = false,
    val availableEnergies: String = "", // Comma-separated enum names (e.g. "GASOLINE_95,DIESEL")
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING"
)

/**
 * Maps Entity to Domain model.
 */
fun ServiceStationEntity.toDomain(): ServiceStation = ServiceStation(
    id = id,
    name = name,
    brand = brand,
    latitude = latitude,
    longitude = longitude,
    address = address,
    isFavorite = isFavorite,
    availableEnergies = if (availableEnergies.isBlank()) {
        emptyList()
    } else {
        availableEnergies.split(",")
            .map { name -> FuelType.fromName(name.trim()) }
    },
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.valueOf(syncStatus)
)

/**
 * Maps Domain model to Entity.
 */
fun ServiceStation.toEntity(): ServiceStationEntity = ServiceStationEntity(
    id = id,
    name = name,
    brand = brand,
    latitude = latitude,
    longitude = longitude,
    address = address,
    isFavorite = isFavorite,
    availableEnergies = availableEnergies.joinToString(",") { it.name },
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name
)
