package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.SyncStatus
import es.joshluq.kmsafe.domain.repository.AuthRepository
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
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
                var currentRemoteId = contract.id
                // 1. If it's a local numeric ID or PENDING, it needs to be created/updated on server
                if (contract.id.length < 5 || contract.syncStatus == SyncStatus.PENDING) {
                    logger.d("MigrateLocalData", "Synchronizing contract: ${contract.vehicleName}")
                    currentRemoteId = rentingRepository.saveContract(contract).first()
                    logger.i("MigrateLocalData", "Contract migrated/synced. ID: $currentRemoteId")
                }

                // 2. Always check for PENDING records for EVERY contract
                val records = historyRepository.getHistory(currentRemoteId).first()
                val pendingRecords = records.filter { it.syncStatus == SyncStatus.PENDING }

                if (pendingRecords.isNotEmpty()) {
                    logger.d(
                        "MigrateLocalData",
                        "Found ${pendingRecords.size} pending records for $currentRemoteId. Syncing..."
                    )
                    for (record in pendingRecords) {
                        historyRepository.saveRecord(record)
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
