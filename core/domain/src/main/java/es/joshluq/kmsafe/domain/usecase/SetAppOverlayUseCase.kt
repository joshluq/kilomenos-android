package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.AppOverlayState
import es.joshluq.kmsafe.domain.repository.AppOverlayRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Domain interface to update the global application HUD overlay state.
 */
interface SetAppOverlayUseCase : FlowUseCase<SetAppOverlayUseCase.Input, SetAppOverlayUseCase.Output> {

    data class Input(val state: AppOverlayState) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Success : Output
    }
}

class SetAppOverlayUseCaseImpl @Inject constructor(
    private val appOverlayRepository: AppOverlayRepository
) : SetAppOverlayUseCase {

    override fun invoke(input: SetAppOverlayUseCase.Input): Flow<SetAppOverlayUseCase.Output> = flow {
        if (input.state is AppOverlayState.None) {
            appOverlayRepository.clearOverlay()
        } else {
            appOverlayRepository.setOverlay(input.state)
        }
        emit(SetAppOverlayUseCase.Output.Success)
    }
}
