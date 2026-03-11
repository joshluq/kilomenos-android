package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class DeleteContractUseCase @Inject constructor(
    private val repository: RentingRepository,
    private val logger: LoggerKit
) : FlowUseCase<DeleteContractUseCase.Input, DeleteContractUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("DeleteContractUseCase", "Deleting contract ID: ${input.id}")
        return repository.deleteContract(input.id)
            .map {
                logger.i("DeleteContractUseCase", "Contract deleted successfully")
                Output.Success as Output
            }
            .onStart { emit(Output.Progress) }
            .catch {
                logger.e("DeleteContractUseCase", "Failed to delete contract", it)
                emit(Output.Failure)
            }
    }

    data class Input(val id: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        object Success : Output
    }
}
