package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Domain interface to register for background activity transitions.
 */
interface StartAutoTrackingUseCase : FlowUseCase<StartAutoTrackingUseCase.Input, StartAutoTrackingUseCase.Output> {

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Success : Output
    }
}

class StartAutoTrackingUseCaseImpl @Inject constructor(
    private val repository: TrackingRepository
) : StartAutoTrackingUseCase {

    override fun invoke(input: StartAutoTrackingUseCase.Input): Flow<StartAutoTrackingUseCase.Output> = flow {
        repository.startAutoTracking()
        emit(StartAutoTrackingUseCase.Output.Success)
    }
}
