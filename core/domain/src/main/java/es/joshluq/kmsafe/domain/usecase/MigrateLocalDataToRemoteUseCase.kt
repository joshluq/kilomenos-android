package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.SyncStatus
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.repository.AuthRepository
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Domain interface to push all PENDING local contracts and odometer records to the remote server.
 * This is triggered after a user upgrades to PREMIUM or when network is restored.
 */
interface MigrateLocalDataToRemoteUseCase : FlowUseCase<MigrateLocalDataToRemoteUseCase.Input, MigrateLocalDataToRemoteUseCase.Output> {

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data object Success : Output
    }
}

class MigrateLocalDataToRemoteUseCaseImpl @Inject constructor(
    private val rentingRepository: RentingRepository,
    private val historyRepository: HistoryRepository,
    private val fuelRepository: FuelExpenseRepository,
    private val stationRepository: ServiceStationRepository,
    private val authRepository: AuthRepository,
    private val logger: LoggerKit
) : MigrateLocalDataToRemoteUseCase {

    override fun invoke(input: MigrateLocalDataToRemoteUseCase.Input): Flow<MigrateLocalDataToRemoteUseCase.Output> = flow {
        logger.d("MigrateLocalData", "Starting migration of PENDING local data to remote")

        // 0. Ensure user session is refreshed to pick up PREMIUM level
        val entitlements = authRepository.getEntitlements().first()
        if (entitlements.subscriptionLevel == SubscriptionLevel.FREE) {
            logger.d("MigrateLocalData", "Aborting migration: User is FREE and cloud sync is disabled")
            emit(MigrateLocalDataToRemoteUseCase.Output.Success as MigrateLocalDataToRemoteUseCase.Output)
            return@flow
        }

        var hasFailures = false

        // 1. Migrate Stations First (Expenses depend on them)
        try {
            val allStations = stationRepository.getAllStations().first()
            val pendingStations = allStations.filter { it.syncStatus == SyncStatus.PENDING }
            if (pendingStations.isNotEmpty()) {
                logger.d("MigrateLocalData", "Syncing batch of ${pendingStations.size} pending stations")
                stationRepository.syncStationBatch(pendingStations).first()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            logger.e("MigrateLocalData", "Failed to migrate stations", e)
            hasFailures = true
        }

        // 2. Migrate ALL Contracts
        val allContracts = rentingRepository.getAllContracts().first()
        logger.d("MigrateLocalData", "Found ${allContracts.size} total contracts to evaluate")

        for (contract in allContracts) {
            try {
                var currentRemoteId = contract.id
                // If it's a local numeric ID or PENDING, it needs to be created/updated on server
                if (contract.id.length < 5 || contract.syncStatus == SyncStatus.PENDING) {
                    logger.d("MigrateLocalData", "Synchronizing contract: ${contract.vehicleName}")
                    currentRemoteId = rentingRepository.saveContract(contract).first()
                    logger.i("MigrateLocalData", "Contract migrated/synced. ID: $currentRemoteId")
                }

                // Sync Odometer Records
                val records = historyRepository.getHistory(currentRemoteId).first()
                val pendingRecords = records.filter { it.syncStatus == SyncStatus.PENDING }

                if (pendingRecords.isNotEmpty()) {
                    logger.d("MigrateLocalData", "Syncing ${pendingRecords.size} pending records for $currentRemoteId")
                    for (record in pendingRecords) {
                        // Skip initial record as it is automatically created by the server during contract creation
                        if (record.isInitialRecord) {
                            logger.d("MigrateLocalData", "Skipping initial record migration for ID: ${record.id}")
                            continue
                        }
                        historyRepository.saveRecord(record)
                    }
                }

                // Sync Fuel Expenses
                val fuelExpenses = fuelRepository.getExpensesByVehicle(currentRemoteId).first()
                val pendingExpenses = fuelExpenses.filter { it.syncStatus == SyncStatus.PENDING }

                if (pendingExpenses.isNotEmpty()) {
                    logger.d("MigrateLocalData", "Syncing ${pendingExpenses.size} pending fuel expenses for $currentRemoteId")
                    for (expense in pendingExpenses) {
                        fuelRepository.saveExpense(expense).first()
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                logger.e("MigrateLocalData", "Failed to migrate contract ${contract.vehicleName}", e)
                hasFailures = true
            }
        }

        if (hasFailures) {
            logger.w("MigrateLocalData", "Migration process completed with partial failures")
            emit(MigrateLocalDataToRemoteUseCase.Output.Failure as MigrateLocalDataToRemoteUseCase.Output)
        } else {
            logger.i("MigrateLocalData", "Migration process completed successfully")
            emit(MigrateLocalDataToRemoteUseCase.Output.Success as MigrateLocalDataToRemoteUseCase.Output)
        }
    }.onStart { emit(MigrateLocalDataToRemoteUseCase.Output.Progress as MigrateLocalDataToRemoteUseCase.Output) }
        .catch {
            if (it is kotlinx.coroutines.CancellationException) throw it
            logger.e("MigrateLocalData", "Critical failure during migration", it)
            emit(MigrateLocalDataToRemoteUseCase.Output.Failure as MigrateLocalDataToRemoteUseCase.Output)
        }
}
