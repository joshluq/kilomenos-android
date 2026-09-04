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

/**
 * Domain interface to delete a renting contract.
 */
interface DeleteContractUseCase : FlowUseCase<DeleteContractUseCase.Input, DeleteContractUseCase.Output> {

    data class Input(val id: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        object Success : Output
    }
}

class DeleteContractUseCaseImpl @Inject constructor(
    private val repository: RentingRepository,
    private val logger: LoggerKit
) : DeleteContractUseCase {

    override fun invoke(input: DeleteContractUseCase.Input): Flow<DeleteContractUseCase.Output> {
        logger.d("DeleteContractUseCase", "Deleting contract ID: ${input.id}")
        return repository.deleteContract(input.id)
            .map {
                logger.i("DeleteContractUseCase", "Contract deleted successfully")
                DeleteContractUseCase.Output.Success as DeleteContractUseCase.Output
            }
            .onStart { emit(DeleteContractUseCase.Output.Progress) }
            .catch {
                logger.e("DeleteContractUseCase", "Failed to delete contract", it)
                emit(DeleteContractUseCase.Output.Failure)
            }
    }
}
