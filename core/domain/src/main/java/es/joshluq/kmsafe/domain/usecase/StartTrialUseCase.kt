package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.KmException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case to initiate the 7-day trial period.
 */
class StartTrialUseCase @Inject constructor(
    private val repository: EntitlementsRepository
) : FlowUseCase<StartTrialUseCase.Input, StartTrialUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        return repository.startTrial(input.deviceFingerprint)
            .map { Output.Success(it) as Output }
            .catch { throwable ->
                val error = (throwable as? KmException)?.error ?: KmError.UnknownError
                emit(Output.Failure(error))
            }
    }

    data class Input(val deviceFingerprint: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val entitlements: Entitlements) : Output
        data class Failure(val error: KmError) : Output
    }
}
