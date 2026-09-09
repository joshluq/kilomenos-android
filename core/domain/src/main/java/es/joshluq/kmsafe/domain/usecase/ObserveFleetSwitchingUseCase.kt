package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.FleetSwitchingState
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Domain interface to observe the global fleet vehicle switching state.
 */
interface ObserveFleetSwitchingUseCase :
    FlowUseCase<ObserveFleetSwitchingUseCase.Input, ObserveFleetSwitchingUseCase.Output> {

    data object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val state: FleetSwitchingState) : Output
    }
}

class ObserveFleetSwitchingUseCaseImpl @Inject constructor(
    private val rentingRepository: RentingRepository
) : ObserveFleetSwitchingUseCase {

    override fun invoke(input: ObserveFleetSwitchingUseCase.Input): Flow<ObserveFleetSwitchingUseCase.Output> {
        return rentingRepository.observeFleetSwitching().map {
            ObserveFleetSwitchingUseCase.Output.Success(it)
        }
    }
}
