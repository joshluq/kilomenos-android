package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case to initiate the 7-day trial period.
 */
class StartTrialUseCase @Inject constructor(
    private val repository: EntitlementsRepository
) : FlowUseCase<StartTrialUseCase.Input, StartTrialUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        return repository.startTrial(input.deviceFingerprint).map { Output.Success(it) }
    }

    data class Input(val deviceFingerprint: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val entitlements: Entitlements) : Output
        data class Failure(val message: String) : Output
    }
}
