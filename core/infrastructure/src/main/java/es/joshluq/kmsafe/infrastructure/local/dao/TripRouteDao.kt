package es.joshluq.kmsafe.infrastructure.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import es.joshluq.kmsafe.infrastructure.local.entity.TripRouteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for trip routes.
 */
@Dao
interface TripRouteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: TripRouteEntity)

    @Query("SELECT * FROM trip_route WHERE recordId = :recordId")
    fun getRouteByRecordId(recordId: String): Flow<TripRouteEntity?>

    @Query("SELECT * FROM trip_route WHERE recordId = :recordId")
    suspend fun getRouteByRecordIdSync(recordId: String): TripRouteEntity?

    @Query("DELETE FROM trip_route WHERE recordId = :recordId")
    suspend fun deleteRouteByRecordId(recordId: String)

    /**
     * Deletes all trip routes from the database.
     */
    @Query("DELETE FROM trip_route")
    suspend fun clearAllRoutes()
}
