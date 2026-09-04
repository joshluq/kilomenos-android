package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Domain interface to synchronize odometer records for a specific contract.
 */
interface SyncHistoryUseCase : FlowUseCase<SyncHistoryUseCase.Input, SyncHistoryUseCase.Output> {
    data class Input(val contractId: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Success : Output
        data class Failure(val message: String) : Output
    }
}

class SyncHistoryUseCaseImpl @Inject constructor(
    private val repository: HistoryRepository,
    private val logger: LoggerKit
) : SyncHistoryUseCase {

    override fun invoke(input: SyncHistoryUseCase.Input): Flow<SyncHistoryUseCase.Output> {
        logger.d("SyncHistoryUseCase", "Executing history sync for contract: ${input.contractId}")
        return repository.syncHistory(input.contractId)
            .map {
                logger.i("SyncHistoryUseCase", "History sync completed")
                SyncHistoryUseCase.Output.Success as SyncHistoryUseCase.Output
            }
            .onStart { emit(SyncHistoryUseCase.Output.Progress) }
            .catch {
                logger.e("SyncHistoryUseCase", "History sync failed", it)
                emit(SyncHistoryUseCase.Output.Failure(it.message ?: "Unknown sync error"))
            }
    }
}
