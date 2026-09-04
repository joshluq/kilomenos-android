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
 * Domain interface to retrieve the current entitlements for the user and device.
 */
interface GetEntitlementsUseCase : FlowUseCase<GetEntitlementsUseCase.Input, GetEntitlementsUseCase.Output> {

    data class Input(
        val deviceFingerprint: String,
        val forceRefresh: Boolean = false
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val entitlements: Entitlements) : Output
        data class Failure(val message: String) : Output
    }
}

class GetEntitlementsUseCaseImpl @Inject constructor(
    private val repository: EntitlementsRepository
) : GetEntitlementsUseCase {

    override fun invoke(input: GetEntitlementsUseCase.Input): Flow<GetEntitlementsUseCase.Output> {
        return if (input.forceRefresh) {
            repository.getEntitlements(input.deviceFingerprint, forceRefresh = true).map { GetEntitlementsUseCase.Output.Success(it) }
        } else {
            repository.observeEntitlements().map { GetEntitlementsUseCase.Output.Success(it) }
        }
    }
}
