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
 * Use case to handle the update of an existing renting contract.
 */
class UpdateContractUseCase @Inject constructor(
    private val repository: RentingRepository,
    private val logger: LoggerKit
) : FlowUseCase<UpdateContractUseCase.Input, UpdateContractUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("UpdateContractUseCase", "Updating contract: ${input.contract.id}")
        return repository.updateContract(input.contract)
            .map {
                logger.i("UpdateContractUseCase", "Contract updated successfully")
                Output.Success as Output
            }
            .onStart { emit(Output.Progress) }
            .catch {
                logger.e("UpdateContractUseCase", "Failed to update contract", it)
                emit(Output.Failure)
            }
    }

    data class Input(val contract: RentingContract) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        object Success : Output
    }
}
