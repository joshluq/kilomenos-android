package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Domain interface to clear the current tracked trip data.
 */
interface ClearTrackingUseCase : FlowUseCase<ClearTrackingUseCase.Input, ClearTrackingUseCase.Output> {

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Success : Output
    }
}

class ClearTrackingUseCaseImpl @Inject constructor(
    private val repository: TrackingRepository
) : ClearTrackingUseCase {

    override fun invoke(input: ClearTrackingUseCase.Input): Flow<ClearTrackingUseCase.Output> = flow {
        repository.clear()
        emit(ClearTrackingUseCase.Output.Success)
    }
}
