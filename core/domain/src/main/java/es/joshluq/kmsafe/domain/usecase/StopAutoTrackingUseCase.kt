package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Domain interface to unregister from background activity transitions.
 */
interface StopAutoTrackingUseCase : FlowUseCase<StopAutoTrackingUseCase.Input, StopAutoTrackingUseCase.Output> {

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Success : Output
    }
}

class StopAutoTrackingUseCaseImpl @Inject constructor(
    private val repository: TrackingRepository
) : StopAutoTrackingUseCase {

    override fun invoke(input: StopAutoTrackingUseCase.Input): Flow<StopAutoTrackingUseCase.Output> = flow {
        repository.stopAutoTracking()
        emit(StopAutoTrackingUseCase.Output.Success)
    }
}
