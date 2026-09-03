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
 * Domain interface for fetching the active renting contract.
 */
interface GetRentingContractUseCase : FlowUseCase<GetRentingContractUseCase.Input, GetRentingContractUseCase.Output> {

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        data class Success(val contract: RentingContract) : Output
    }
}

class GetRentingContractUseCaseImpl @Inject constructor(
    private val repository: RentingRepository,
    private val logger: LoggerKit
) : GetRentingContractUseCase {

    override fun invoke(input: GetRentingContractUseCase.Input): Flow<GetRentingContractUseCase.Output> {
        logger.d("GetRentingContractUseCase", "Fetching active contract")
        return repository.getContract()
            .map { contract ->
                if (contract == null) {
                    logger.w("GetRentingContractUseCase", "No contract found")
                    GetRentingContractUseCase.Output.Failure
                } else {
                    logger.i("GetRentingContractUseCase", "Contract found: ${contract.vehicleName}")
                    GetRentingContractUseCase.Output.Success(contract)
                }
            }
            .onStart { emit(GetRentingContractUseCase.Output.Progress) }
            .catch {
                logger.e("GetRentingContractUseCase", "Error fetching contract", it)
                emit(GetRentingContractUseCase.Output.Failure)
            }
    }
}
