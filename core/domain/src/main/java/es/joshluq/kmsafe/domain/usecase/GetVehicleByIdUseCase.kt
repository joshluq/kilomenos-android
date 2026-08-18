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

class GetVehicleByIdUseCase @Inject constructor(
    private val repository: RentingRepository,
    private val logger: LoggerKit
) : FlowUseCase<GetVehicleByIdUseCase.Input, GetVehicleByIdUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("GetVehicleByIdUseCase", "Fetching vehicle with ID: ${input.id}")
        return repository.getContractById(input.id)
            .map { contract ->
                if (contract == null) {
                    logger.w("GetVehicleByIdUseCase", "Vehicle not found")
                    Output.Failure
                } else {
                    logger.i("GetVehicleByIdUseCase", "Vehicle found: ${contract.vehicleName}")
                    Output.Success(contract)
                }
            }
            .onStart { emit(Output.Progress) }
            .catch {
                logger.e("GetVehicleByIdUseCase", "Error fetching vehicle", it)
                emit(Output.Failure)
            }
    }

    data class Input(val id: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        data class Success(val contract: RentingContract) : Output
    }
}
