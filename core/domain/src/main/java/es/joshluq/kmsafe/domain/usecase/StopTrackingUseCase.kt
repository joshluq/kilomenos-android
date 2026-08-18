package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case to stop the current tracking session in the repository.
 */
class StopTrackingUseCase @Inject constructor(
    private val repository: TrackingRepository
) : FlowUseCase<StopTrackingUseCase.Input, StopTrackingUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> = flow {
        repository.stopTracking()
        emit(Output.Success)
    }

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Success : Output
    }
}
