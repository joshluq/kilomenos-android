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

class GetRentingContractUseCase @Inject constructor(
    private val repository: RentingRepository,
    private val logger: LoggerKit
) : FlowUseCase<GetRentingContractUseCase.Input, GetRentingContractUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("GetRentingContractUseCase", "Fetching active contract")
        return repository.getContract()
            .map { contract ->
                if (contract == null) {
                    logger.w("GetRentingContractUseCase", "No contract found")
                    Output.Failure
                } else {
                    logger.i("GetRentingContractUseCase", "Contract found: ${contract.vehicleName}")
                    Output.Success(contract)
                }
            }
            .onStart { emit(Output.Progress) }
            .catch {
                logger.e("GetRentingContractUseCase", "Error fetching contract", it)
                emit(Output.Failure)
            }
    }

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        data class Success(val contract: RentingContract) : Output
    }
}
