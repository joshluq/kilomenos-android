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
 * Domain interface to fetch all vehicle contracts.
 */
interface GetAllContractsUseCase : FlowUseCase<GetAllContractsUseCase.Input, GetAllContractsUseCase.Output> {

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        data class Success(val contracts: List<RentingContract>) : Output
    }
}

class GetAllContractsUseCaseImpl @Inject constructor(
    private val repository: RentingRepository,
    private val logger: LoggerKit
) : GetAllContractsUseCase {

    override fun invoke(input: GetAllContractsUseCase.Input): Flow<GetAllContractsUseCase.Output> {
        logger.d("GetAllContractsUseCase", "Fetching all contracts")
        return repository.getAllContracts()
            .map { contracts ->
                logger.i("GetAllContractsUseCase", "Retrieved ${contracts.size} contracts")
                GetAllContractsUseCase.Output.Success(contracts) as GetAllContractsUseCase.Output
            }
            .onStart { emit(GetAllContractsUseCase.Output.Progress) }
            .catch {
                logger.e("GetAllContractsUseCase", "Error fetching contracts", it)
                emit(GetAllContractsUseCase.Output.Failure)
            }
    }
}
