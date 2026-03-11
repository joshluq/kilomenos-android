package es.joshluq.kmsafe.domain.repository

import es.joshluq.kmsafe.domain.model.RentingContract
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing the renting contract configuration.
 */
interface RentingRepository {

    /**
     * Saves the renting contract configuration to persistent storage.
     *
     * @param contract The [RentingContract] to persist.
     * @return A [Flow] emitting the ID of the saved contract.
     */
    fun saveContract(contract: RentingContract): Flow<String>

    /**
     * Updates an existing renting contract.
     *
     * @param contract The [RentingContract] to update.
     */
    fun updateContract(contract: RentingContract): Flow<Unit>

    /**
     * Retrieves the currently selected renting contract.
     *
     * @return A [Flow] emitting the [RentingContract] if it exists, or null if not yet configured.
     */
    fun getContract(): Flow<RentingContract?>

    /**
     * Retrieves a specific renting contract by its ID.
     *
     * @param id The ID of the contract.
     * @return A [Flow] emitting the [RentingContract] if it exists, or null.
     */
    fun getContractById(id: String): Flow<RentingContract?>

    /**
     * Retrieves all registered renting contracts.
     *
     * @return A [Flow] emitting the list of [RentingContract].
     */
    fun getAllContracts(): Flow<List<RentingContract>>

    /**
     * Retrieves all contracts that are pending synchronization.
     */
    suspend fun getPendingContracts(): List<RentingContract>

    /**
     * Selects a contract as the active one.
     *
     * @param id The ID of the contract to select.
     */
    fun selectContract(id: String): Flow<Unit>

    /**
     * Deletes a renting contract and its associated records.
     *
     * @param id The ID of the contract to delete.
     */
    fun deleteContract(id: String): Flow<Unit>

    /**
     * Synchronizes contracts with the remote server.
     *
     * @return A [Flow] emitting the list of synchronized [RentingContract].
     */
    fun syncContracts(): Flow<List<RentingContract>>

    /**
     * Uploads a vehicle image to remote storage.
     *
     * @param imageBytes The binary data of the image.
     * @param fileName The name of the file to save.
     * @return A [Flow] emitting the public URL of the uploaded image.
     */
    fun uploadVehicleImage(imageBytes: ByteArray, fileName: String): Flow<String>

    /**
     * Returns the user ID of the owner of the current local database.
     */
    suspend fun getDatabaseOwnerId(): String?

    /**
     * Clears all local application data (DB).
     */
    fun clearAllLocalData(): Flow<Unit>
}
