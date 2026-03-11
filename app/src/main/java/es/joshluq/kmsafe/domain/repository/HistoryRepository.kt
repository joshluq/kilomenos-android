package es.joshluq.kmsafe.domain.repository

import es.joshluq.kmsafe.domain.model.OdometerRecord
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing odometer history records.
 */
interface HistoryRepository {

    /**
     * Retrieves all odometer records for a specific contract ordered by timestamp descending.
     *
     * @param contractId The ID of the contract.
     */
    fun getHistory(contractId: String): Flow<List<OdometerRecord>>

    /**
     * Retrieves all records that are pending synchronization.
     */
    suspend fun getPendingRecords(): List<OdometerRecord>

    /**
     * Inserts a new odometer record.
     */
    suspend fun saveRecord(record: OdometerRecord)

    /**
     * Updates an existing odometer record.
     */
    suspend fun updateRecord(record: OdometerRecord)

    /**
     * Deletes an existing odometer record.
     */
    suspend fun deleteRecord(record: OdometerRecord)

    /**
     * Synchronizes odometer records for a specific contract with the remote server.
     */
    fun syncHistory(contractId: String): Flow<Unit>
}
