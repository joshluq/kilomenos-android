package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Domain interface to handle the update of an existing renting contract.
 */
interface UpdateContractUseCase : FlowUseCase<UpdateContractUseCase.Input, UpdateContractUseCase.Output> {

    data class Input(val contract: RentingContract) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        object Success : Output
    }
}

class UpdateContractUseCaseImpl @Inject constructor(
    private val repository: RentingRepository,
    private val logger: LoggerKit
) : UpdateContractUseCase {

    override fun invoke(input: UpdateContractUseCase.Input): Flow<UpdateContractUseCase.Output> {
        logger.d("UpdateContractUseCase", "Updating contract: ${input.contract.id}")
        return repository.updateContract(input.contract)
            .map {
                logger.i("UpdateContractUseCase", "Contract updated successfully")
                UpdateContractUseCase.Output.Success as UpdateContractUseCase.Output
            }
            .onStart { emit(UpdateContractUseCase.Output.Progress) }
            .catch {
                logger.e("UpdateContractUseCase", "Failed to update contract", it)
                emit(UpdateContractUseCase.Output.Failure)
            }
    }
}
