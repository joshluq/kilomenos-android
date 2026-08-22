package es.joshluq.kmsafe.domain.model

/**
 * Domain model representing a physical service station or EV charging location.
 *
 * @property id Unique UUID v4 identifying the station.
 * @property name User-facing or commercial name of the station.
 * @property brand Commercial brand (e.g. Repsol, Cepsa, Tesla Supercharger, Iberdrola).
 * @property latitude GPS latitude coordinate.
 * @property longitude GPS longitude coordinate.
 * @property address Physical street or road address.
 * @property isFavorite Whether the user has marked this station as preferred.
 * @property availableEnergies List of fuel/energy types supported at this location.
 */
data class ServiceStation(
    val id: String,
    val name: String,
    val brand: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val isFavorite: Boolean = false,
    val availableEnergies: List<FuelType> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
