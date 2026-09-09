package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.AppOverlayState
import es.joshluq.kmsafe.domain.repository.AppOverlayRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Domain interface to observe the global application HUD overlay state.
 */
interface ObserveAppOverlayUseCase :
    FlowUseCase<ObserveAppOverlayUseCase.Input, ObserveAppOverlayUseCase.Output> {

    data object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val state: AppOverlayState) : Output
    }
}

class ObserveAppOverlayUseCaseImpl @Inject constructor(
    private val appOverlayRepository: AppOverlayRepository
) : ObserveAppOverlayUseCase {

    override fun invoke(input: ObserveAppOverlayUseCase.Input): Flow<ObserveAppOverlayUseCase.Output> {
        return appOverlayRepository.observeOverlay().map {
            ObserveAppOverlayUseCase.Output.Success(it)
        }
    }
}
