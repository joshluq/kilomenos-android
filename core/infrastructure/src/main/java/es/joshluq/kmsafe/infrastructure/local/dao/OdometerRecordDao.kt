package es.joshluq.kmsafe.infrastructure.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import es.joshluq.kmsafe.infrastructure.local.entity.OdometerRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the OdometerRecord table.
 */
@Dao
interface OdometerRecordDao {

    @Query("SELECT * FROM odometer_record WHERE contractId = :contractId ORDER BY timestamp DESC")
    fun getAllRecords(contractId: String): Flow<List<OdometerRecordEntity>>

    @Query("SELECT * FROM odometer_record WHERE contractId = :contractId")
    suspend fun getRecordsByContractIdSync(contractId: String): List<OdometerRecordEntity>

    @Query("SELECT * FROM odometer_record")
    fun getAllRecordsForBackup(): Flow<List<OdometerRecordEntity>>

    /**
     * Retrieves all records that are pending synchronization for a specific contract.
     */
    @Query("SELECT * FROM odometer_record WHERE syncStatus = 'PENDING'")
    suspend fun getAllPendingRecords(): List<OdometerRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: OdometerRecordEntity)

    @Delete
    suspend fun deleteRecord(record: OdometerRecordEntity)

    @Query("DELETE FROM odometer_record WHERE contractId = :contractId")
    suspend fun deleteRecordsByContractId(contractId: String)

    /**
     * Updates the contractId of all records associated with an old contractId.
     * This is crucial during migration when a local ID is swapped with a remote one.
     */
    @Query("UPDATE odometer_record SET contractId = :newId WHERE contractId = :oldId")
    suspend fun updateContractId(oldId: String, newId: String)

    @Query("SELECT * FROM odometer_record WHERE id = :recordId")
    suspend fun getRecordByIdSync(recordId: String): OdometerRecordEntity?

    /**
     * Updates the hasRoute flag for a specific record.
     */
    @Query("UPDATE odometer_record SET hasRoute = :hasRoute WHERE id = :recordId")
    suspend fun updateHasRoute(recordId: String, hasRoute: Boolean)

    /**
     * Deletes all odometer records from the database.
     */
    @Query("DELETE FROM odometer_record")
    suspend fun clearAllRecords()
}
