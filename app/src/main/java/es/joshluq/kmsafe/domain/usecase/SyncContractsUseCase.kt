package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Use case to synchronize renting contracts from remote to local storage.
 * Performs a "Deep Sync" by also fetching the history of the active contract.
 */
class SyncContractsUseCase @Inject constructor(
    private val repository: RentingRepository,
    private val historyRepository: HistoryRepository,
    private val logger: LoggerKit
) : FlowUseCase<SyncContractsUseCase.Input, SyncContractsUseCase.Output> {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: Input): Flow<Output> {
        logger.d("SyncContractsUseCase", "Executing Deep Sync")
        return repository.syncContracts()
            .flatMapConcat { contracts ->
                val activeContract = contracts.find { it.isSelected }
                if (activeContract != null) {
                    logger.i("SyncContractsUseCase", "Active contract found: ${activeContract.id}. Syncing history.")
                    historyRepository.syncHistory(activeContract.id).map {
                        Output.Success as Output
                    }
                } else {
                    logger.w("SyncContractsUseCase", "No active contract to deep sync")
                    flowOf(Output.Failure("No active contract to deep sync"))
                }
            }
            .onStart { emit(Output.Progress) }
            .catch {
                logger.e("SyncContractsUseCase", "Deep Sync failed", it)
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
