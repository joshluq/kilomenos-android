package es.joshluq.kmsafe.infrastructure.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import es.joshluq.kmsafe.infrastructure.local.entity.FuelExpenseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for fuel and EV charging expenses.
 */
@Dao
interface FuelExpenseDao {

    @Query("SELECT * FROM fuel_expenses WHERE vehicleId = :vehicleId ORDER BY timestamp DESC")
    fun getExpensesByVehicle(vehicleId: String): Flow<List<FuelExpenseEntity>>

    @Query("SELECT * FROM fuel_expenses WHERE id = :id LIMIT 1")
    fun getExpenseById(id: String): Flow<FuelExpenseEntity?>

    @Query("SELECT * FROM fuel_expenses WHERE stationId = :stationId AND fuelType = :fuelType ORDER BY timestamp DESC")
    fun getExpensesByStationAndFuel(stationId: String, fuelType: String): Flow<List<FuelExpenseEntity>>

    @Query("SELECT * FROM fuel_expenses WHERE stationId = :stationId ORDER BY timestamp DESC")
    fun getExpensesByStation(stationId: String): Flow<List<FuelExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: FuelExpenseEntity)

    @Query("DELETE FROM fuel_expenses WHERE id = :id")
    suspend fun deleteExpense(id: String)

    @Query("DELETE FROM fuel_expenses WHERE vehicleId = :vehicleId")
    suspend fun deleteExpensesByVehicle(vehicleId: String)

    /**
     * Updates the vehicleId of all expenses associated with an old vehicleId.
     * Crucial for identity resolution (ID Swap) during synchronization.
     */
    @Query("UPDATE fuel_expenses SET vehicleId = :newId WHERE vehicleId = :oldId")
    suspend fun updateVehicleId(oldId: String, newId: String)

    /**
     * Updates the stationId of all expenses associated with an old stationId.
     * Crucial for identity resolution (ID Swap) during synchronization.
     */
    @Query("UPDATE fuel_expenses SET stationId = :newId WHERE stationId = :oldId")
    suspend fun updateStationId(oldId: String, newId: String)

    @Query("UPDATE fuel_expenses SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("SELECT COUNT(*) FROM fuel_expenses")
    suspend fun getExpenseCount(): Int

    @Query("DELETE FROM fuel_expenses")
    suspend fun clearAllExpenses()
}
