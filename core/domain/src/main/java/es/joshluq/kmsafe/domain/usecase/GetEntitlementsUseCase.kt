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
 * Use case to retrieve the current entitlements for the user and device.
 */
class GetEntitlementsUseCase @Inject constructor(
    private val repository: EntitlementsRepository
) : FlowUseCase<GetEntitlementsUseCase.Input, GetEntitlementsUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        return if (input.forceRefresh) {
            repository.getEntitlements(input.deviceFingerprint, forceRefresh = true).map { Output.Success(it) }
        } else {
            repository.observeEntitlements().map { Output.Success(it) }
        }
    }

    data class Input(
        val deviceFingerprint: String,
        val forceRefresh: Boolean = false
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val entitlements: Entitlements) : Output
        data class Failure(val message: String) : Output
    }
}
