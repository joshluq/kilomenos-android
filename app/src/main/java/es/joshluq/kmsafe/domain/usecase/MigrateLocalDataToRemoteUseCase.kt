package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.SyncStatus
import es.joshluq.kmsafe.domain.repository.AuthRepository
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Use case to push all PENDING local contracts and odometer records to the remote server.
 * This is triggered after a user upgrades to PREMIUM or when network is restored.
 */
class MigrateLocalDataToRemoteUseCase @Inject constructor(
    private val rentingRepository: RentingRepository,
    private val historyRepository: HistoryRepository,
    private val authRepository: AuthRepository,
    private val logger: LoggerKit
) : FlowUseCase<MigrateLocalDataToRemoteUseCase.Input, MigrateLocalDataToRemoteUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> = flow {
        logger.d("MigrateLocalData", "Starting migration of PENDING local data to remote")
        
        // 0. Ensure user session is refreshed to pick up PREMIUM level
        authRepository.getCurrentUser().first()
        
        // 1. Migrate ALL Contracts
        // We migrate all because some might be SYNCED locally but missing remotely 
        // due to previous inconsistent states. saveContract handles the sync logic.
        val allContracts = rentingRepository.getAllContracts().first()
        logger.d("MigrateLocalData", "Found ${allContracts.size} total contracts to evaluate")

        for (contract in allContracts) {
            try {
                // If it's a local numeric ID, it needs to be created on server
                // If it's already a UUID, saveContract logic will handle it or we can skip if SYNCED
                if (contract.id.length < 5 || contract.syncStatus == SyncStatus.PENDING) {
                    logger.d("MigrateLocalData", "Synchronizing contract: ${contract.vehicleName}")
                    rentingRepository.saveContract(contract).collect { remoteId ->
                        logger.i("MigrateLocalData", "Contract migrated/synced. ID: $remoteId")
                        
                        // 2. Migrate records for THIS contract to ensure they use the correct remoteId
                        val records = historyRepository.getHistory(remoteId).first()
                        val pendingRecords = records.filter { it.syncStatus == SyncStatus.PENDING }
                        logger.d("MigrateLocalData", "Found ${pendingRecords.size} pending records for $remoteId")
                        
                        for (record in pendingRecords) {
                            historyRepository.saveRecord(record)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.e("MigrateLocalData", "Failed to migrate contract ${contract.vehicleName}", e)
            }
        }

        logger.i("MigrateLocalData", "Migration process completed")
        emit(Output.Success as Output)
    }.onStart { emit(Output.Progress as Output) }
    .catch {
        logger.e("MigrateLocalData", "Critical failure during migration", it)
        emit(Output.Failure as Output)
    }

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data object Success : Output
    }
}
