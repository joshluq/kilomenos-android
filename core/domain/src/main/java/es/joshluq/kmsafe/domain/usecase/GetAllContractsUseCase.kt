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

class GetAllContractsUseCase @Inject constructor(
    private val repository: RentingRepository,
    private val logger: LoggerKit
) : FlowUseCase<GetAllContractsUseCase.Input, GetAllContractsUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("GetAllContractsUseCase", "Fetching all contracts")
        return repository.getAllContracts()
            .map { contracts ->
                logger.i("GetAllContractsUseCase", "Retrieved ${contracts.size} contracts")
                Output.Success(contracts) as Output
            }
            .onStart { emit(Output.Progress) }
            .catch {
                logger.e("GetAllContractsUseCase", "Error fetching contracts", it)
                emit(Output.Failure)
            }
    }

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        data class Success(val contracts: List<RentingContract>) : Output
    }
}
