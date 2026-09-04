package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Domain interface to stop the current tracking session in the repository.
 */
interface StopTrackingUseCase : FlowUseCase<StopTrackingUseCase.Input, StopTrackingUseCase.Output> {

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Success : Output
    }
}

class StopTrackingUseCaseImpl @Inject constructor(
    private val repository: TrackingRepository
) : StopTrackingUseCase {

    override fun invoke(input: StopTrackingUseCase.Input): Flow<StopTrackingUseCase.Output> = flow {
        repository.stopTracking()
        emit(StopTrackingUseCase.Output.Success)
    }
}
