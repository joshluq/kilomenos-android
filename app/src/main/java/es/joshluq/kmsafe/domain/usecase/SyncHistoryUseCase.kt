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
 * Use case to synchronize odometer records for a specific contract.
 */
class SyncHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository,
    private val logger: LoggerKit
) : FlowUseCase<SyncHistoryUseCase.Input, SyncHistoryUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("SyncHistoryUseCase", "Executing history sync for contract: ${input.contractId}")
        return repository.syncHistory(input.contractId)
            .map {
                logger.i("SyncHistoryUseCase", "History sync completed")
                Output.Success as Output
            }
            .onStart { emit(Output.Progress) }
            .catch {
                logger.e("SyncHistoryUseCase", "History sync failed", it)
                emit(Output.Failure(it.message ?: "Unknown sync error"))
            }
    }

    data class Input(val contractId: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Success : Output
        data class Failure(val message: String) : Output
    }
}
