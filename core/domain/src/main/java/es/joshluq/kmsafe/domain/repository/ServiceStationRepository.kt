package es.joshluq.kmsafe.domain.repository

import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.domain.model.StationPriceVolatility
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing service stations, EV charging points, and price volatility analytics.
 */
interface ServiceStationRepository {

    /**
     * Observes all service stations saved locally.
     */
    fun getAllStations(): Flow<List<ServiceStation>>

    /**
     * Observes stations marked as favorites by the user.
     */
    fun getFavoriteStations(): Flow<List<ServiceStation>>

    /**
     * Retrieves a station by its ID.
     */
    fun getStationById(id: String): Flow<ServiceStation?>

    /**
     * Saves or updates a service station entry.
     */
    fun saveStation(station: ServiceStation): Flow<String>

    /**
     * Sets or removes the favorite status for a station.
     */
    fun setFavorite(stationId: String, isFavorite: Boolean): Flow<Unit>

    /**
     * Deletes a service station by its ID.
     */
    fun deleteStation(id: String): Flow<Unit>

    /**
     * Deletes all service stations from the database.
     */
    fun clearAllStations(): Flow<Unit>

    /**
     * Synchronizes service stations with the remote server.
     *
     * @return A [Flow] emitting the list of synchronized [ServiceStation].
     */
    fun syncStations(): Flow<List<ServiceStation>>

    /**
     * Synchronizes a batch of service stations with the remote server.
     */
    fun syncStationBatch(stations: List<ServiceStation>): Flow<List<ServiceStation>>

    /**
     * Calculates price volatility metrics and trend comparison for a station and fuel type.
     */
    fun getStationVolatility(stationId: String, fuelType: FuelType): Flow<StationPriceVolatility?>
}
