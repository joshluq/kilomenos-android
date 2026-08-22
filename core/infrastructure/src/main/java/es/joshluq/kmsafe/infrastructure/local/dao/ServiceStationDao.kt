package es.joshluq.kmsafe.infrastructure.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import es.joshluq.kmsafe.infrastructure.local.entity.ServiceStationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for service stations and EV charging points.
 */
@Dao
interface ServiceStationDao {

    @Query("SELECT * FROM service_stations ORDER BY name ASC")
    fun getAllStations(): Flow<List<ServiceStationEntity>>

    @Query("SELECT * FROM service_stations WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteStations(): Flow<List<ServiceStationEntity>>

    @Query("SELECT * FROM service_stations WHERE id = :id LIMIT 1")
    fun getStationById(id: String): Flow<ServiceStationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: ServiceStationEntity)

    @Query("UPDATE service_stations SET isFavorite = :isFavorite WHERE id = :stationId")
    suspend fun updateFavorite(stationId: String, isFavorite: Boolean)

    @Query("UPDATE service_stations SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM service_stations WHERE id = :id")
    suspend fun deleteStation(id: String)

    @Query("SELECT COUNT(*) FROM service_stations")
    suspend fun getStationCount(): Int

    @Query("DELETE FROM service_stations")
    suspend fun clearAllStations()
}
