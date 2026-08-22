package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Use case to synchronize renting contracts from remote to local storage.
 * Performs a "Deep Sync" by also fetching the history of the active contract and user stations.
 */
class SyncContractsUseCase @Inject constructor(
    private val repository: RentingRepository,
    private val historyRepository: HistoryRepository,
    private val stationRepository: ServiceStationRepository,
    private val fuelRepository: FuelExpenseRepository,
    private val logger: LoggerKit
) : FlowUseCase<SyncContractsUseCase.Input, SyncContractsUseCase.Output> {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: Input): Flow<Output> {
        logger.d("SyncContractsUseCase", "Executing Full Deep Sync")
        
        // 1. Sync Stations and Contracts in parallel
        return combine(
            repository.syncContracts(),
            stationRepository.syncStations()
        ) { contracts, _ ->
            contracts
        }.flatMapConcat { contracts ->
            // 2. Sync history for the active contract
            val activeContract = contracts.find { it.isSelected }
            if (activeContract != null) {
                logger.i("SyncContractsUseCase", "Active contract found: ${activeContract.id}. Syncing history and fuel expenses.")
                
                combine(
                    historyRepository.syncHistory(activeContract.id),
                    fuelRepository.syncFuelExpenses(activeContract.id)
                ) { _, _ ->
                    Output.Success as Output
                }
            } else {
                logger.w("SyncContractsUseCase", "No active contract found after sync")
                flowOf(Output.Success as Output)
            }
        }
        .onStart { emit(Output.Progress) }
        .catch {
            logger.e("SyncContractsUseCase", "Full Deep Sync failed", it)
            emit(Output.Failure(it.message ?: "Unknown sync error"))
        }
    }

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Success : Output
        data class Failure(val message: String) : Output
    }
}
