package es.joshluq.kmsafe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import es.joshluq.kmsafe.data.local.entity.RentingContractEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the RentingContract table.
 */
@Dao
interface RentingContractDao {

    /**
     * Inserts or updates the renting contract.
     *
     * @param contract The contract entity to save.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContract(contract: RentingContractEntity)

    /**
     * Observes the currently selected renting contract from the database.
     *
     * @return A [Flow] emitting the contract entity or null if not found.
     */
    @Query("SELECT * FROM renting_contract WHERE isSelected = 1 LIMIT 1")
    fun getSelectedContract(): Flow<RentingContractEntity?>

    /**
     * Observes a specific renting contract from the database by its ID.
     */
    @Query("SELECT * FROM renting_contract WHERE id = :id LIMIT 1")
    fun getContractById(id: String): Flow<RentingContractEntity?>

    /**
     * Retrieves a specific renting contract from the database by its ID (Non-flow).
     */
    @Query("SELECT * FROM renting_contract WHERE id = :id LIMIT 1")
    suspend fun getContractByIdSync(id: String): RentingContractEntity?

    /**
     * Observes all renting contracts from the database.
     *
     * @return A [Flow] emitting the list of contract entities.
     */
    @Query("SELECT * FROM renting_contract ORDER BY id DESC")
    fun getAllContracts(): Flow<List<RentingContractEntity>>

    /**
     * Retrieves all contracts that are pending synchronization.
     */
    @Query("SELECT * FROM renting_contract WHERE syncStatus = 'PENDING'")
    suspend fun getPendingContracts(): List<RentingContractEntity>

    /**
     * Updates the selection status of all contracts.
     * This should be used within a transaction to ensure only one contract is selected.
     */
    @Query("UPDATE renting_contract SET isSelected = CASE WHEN id = :selectedId THEN 1 ELSE 0 END")
    suspend fun updateSelection(selectedId: String)

    /**
     * Deletes a contract by its ID.
     *
     * @param id The ID of the contract to delete.
     */
    @Query("DELETE FROM renting_contract WHERE id = :id")
    suspend fun deleteContract(id: String)

    /**
     * Retrieves the userId of the first contract found.
     * Use this for identity verification during login.
     */
    @Query("SELECT userId FROM renting_contract LIMIT 1")
    suspend fun getFirstContractUserId(): String?

    /**
     * Deletes all contracts from the database.
     */
    @Query("DELETE FROM renting_contract")
    suspend fun clearAllContracts()
}
