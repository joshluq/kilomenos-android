package es.joshluq.kmsafe.infrastructure.repository

import es.joshluq.foundationkit.coroutines.DispatcherProvider
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.infrastructure.local.dao.OdometerRecordDao
import es.joshluq.kmsafe.infrastructure.local.dao.RentingContractDao
import es.joshluq.kmsafe.infrastructure.local.entity.toDomain
import es.joshluq.kmsafe.infrastructure.local.entity.toEntity
import es.joshluq.kmsafe.domain.model.BackupData
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.repository.DataManagementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Implementation of [DataManagementRepository] handling full data backup, restoration, and CSV exports.
 *
 * Implements GDPR/EAA compliance requirements for data portability and complete local data backup.
 *
 * @property contractDao Room DAO for renting contracts.
 * @property historyDao Room DAO for odometer records.
 * @property logger Logger utility.
 * @property dispatchers Dispatcher provider for background I/O and computation.
 */
class DataManagementRepositoryImpl @Inject constructor(
    private val contractDao: RentingContractDao,
    private val historyDao: OdometerRecordDao,
    private val logger: LoggerKit,
    private val dispatchers: DispatcherProvider
) : DataManagementRepository {

    /**
     * Collects all contracts and history records from local Room storage into a single [BackupData] structure.
     */
    override fun getFullBackupData(): Flow<BackupData> {
        return combine(
            contractDao.getAllContracts(),
            historyDao.getAllRecordsForBackup()
        ) { contractEntities, historyEntities ->
            BackupData(
                contracts = contractEntities.map { it.toDomain() },
                history = historyEntities.map { it.toDomain() }
            )
        }.onEach {
            logger.d(
                "DataManagementRepository",
                "Full backup data collected: ${it.contracts.size} contracts, ${it.history.size} records"
            )
        }
    }

    /**
     * Restores all contracts and history records from a provided [BackupData] payload into local Room storage.
     *
     * @param data The backup dataset containing contracts and odometer records.
     */
    override fun restoreFromBackup(data: BackupData): Flow<Unit> = flow {
        logger.i("DataManagementRepository", "Restoring from backup: ${data.contracts.size} contracts")
        data.contracts.forEach { contractDao.insertContract(it.toEntity()) }
        data.history.forEach { historyDao.insertRecord(it.toEntity()) }
        logger.i("DataManagementRepository", "Restore complete")
        emit(Unit)
    }.flowOn(dispatchers.io)

    /**
     * Generates a CSV-formatted string containing all provided odometer records.
     *
     * @param history List of [OdometerRecord] items to format as CSV rows.
     * @return Formatted CSV string.
     */
    override suspend fun generateHistoryCsv(history: List<OdometerRecord>): String = withContext(dispatchers.default) {
        logger.d("DataManagementRepository", "Generating CSV for ${history.size} records")
        val builder = StringBuilder()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        builder.append("ID,ContractID,Timestamp,Date,OdometerValue,IsInitialRecord\n")

        history.forEach { record ->
            builder.append("${record.id},")
            builder.append("${record.contractId},")
            builder.append("${record.timestamp},")
            builder.append("${dateFormatter.format(Date(record.timestamp))},")
            builder.append("${record.odometerValue},")
            builder.append("${record.isInitialRecord}\n")
        }

        builder.toString()
    }
}
