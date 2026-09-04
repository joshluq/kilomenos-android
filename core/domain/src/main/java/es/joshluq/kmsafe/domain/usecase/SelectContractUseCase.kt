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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Domain interface to select the active vehicle contract.
 */
interface SelectContractUseCase : FlowUseCase<SelectContractUseCase.Input, SelectContractUseCase.Output> {

    data class Input(val id: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        object Success : Output
    }
}

class SelectContractUseCaseImpl @Inject constructor(
    private val rentingRepository: RentingRepository,
    private val historyRepository: HistoryRepository,
    private val logger: LoggerKit
) : SelectContractUseCase {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: SelectContractUseCase.Input): Flow<SelectContractUseCase.Output> {
        logger.d("SelectContractUseCase", "Selecting contract ID: ${input.id}")
        return rentingRepository.selectContract(input.id)
            .flatMapLatest {
                // After selecting, trigger a background sync of the history for this vehicle
                historyRepository.syncHistory(input.id).map {
                    logger.i("SelectContractUseCase", "Selection and history sync updated")
                    SelectContractUseCase.Output.Success as SelectContractUseCase.Output
                }
            }
            .onStart { emit(SelectContractUseCase.Output.Progress) }
            .catch {
                logger.e("SelectContractUseCase", "Failed to update selection", it)
                emit(SelectContractUseCase.Output.Failure)
            }
    }
}
