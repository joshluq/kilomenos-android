package es.joshluq.kmsafe.data.repository

import es.joshluq.foundationkit.coroutines.DispatcherProvider
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.data.local.dao.OdometerRecordDao
import es.joshluq.kmsafe.data.local.dao.RentingContractDao
import es.joshluq.kmsafe.data.local.entity.toDomain
import es.joshluq.kmsafe.data.local.entity.toEntity
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

class DataManagementRepositoryImpl @Inject constructor(
    private val contractDao: RentingContractDao,
    private val historyDao: OdometerRecordDao,
    private val logger: LoggerKit,
    private val dispatchers: DispatcherProvider
) : DataManagementRepository {

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

    override fun restoreFromBackup(data: BackupData): Flow<Unit> = flow {
        logger.i("DataManagementRepository", "Restoring from backup: ${data.contracts.size} contracts")
        data.contracts.forEach { contractDao.insertContract(it.toEntity()) }
        data.history.forEach { historyDao.insertRecord(it.toEntity()) }
        logger.i("DataManagementRepository", "Restore complete")
        emit(Unit)
    }.flowOn(dispatchers.io)

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
