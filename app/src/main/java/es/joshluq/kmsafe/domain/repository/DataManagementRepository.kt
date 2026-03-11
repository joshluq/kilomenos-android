package es.joshluq.kmsafe.domain.repository

import es.joshluq.kmsafe.domain.model.BackupData
import es.joshluq.kmsafe.domain.model.OdometerRecord
import kotlinx.coroutines.flow.Flow

/**
 * Contract for managing application data export and import.
 */
interface DataManagementRepository {

    /**
     * Retrieves all application data for backup purposes.
     */
    fun getFullBackupData(): Flow<BackupData>

    /**
     * Overwrites current application data with the provided backup.
     */
    fun restoreFromBackup(data: BackupData): Flow<Unit>

    /**
     * Converts odometer records to a CSV formatted string.
     */
    suspend fun generateHistoryCsv(history: List<OdometerRecord>): String
}
