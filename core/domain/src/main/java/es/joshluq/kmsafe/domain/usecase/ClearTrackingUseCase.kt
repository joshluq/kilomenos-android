package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case to clear the current tracked trip data.
 */
class ClearTrackingUseCase @Inject constructor(
    private val repository: TrackingRepository
) : FlowUseCase<ClearTrackingUseCase.Input, ClearTrackingUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> = flow {
        repository.clear()
        emit(Output.Success)
    }

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Success : Output
    }
}
