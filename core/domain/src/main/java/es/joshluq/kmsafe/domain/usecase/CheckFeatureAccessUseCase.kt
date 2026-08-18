package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case to check if a specific feature is enabled based on user entitlements.
 */
class CheckFeatureAccessUseCase @Inject constructor(
    private val repository: EntitlementsRepository
) : FlowUseCase<CheckFeatureAccessUseCase.Input, CheckFeatureAccessUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        return repository.observeEntitlements().map { entitlements ->
            Output.Success(entitlements.isFeatureActive(input.feature))
        }
    }

    data class Input(val feature: Feature) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val isGranted: Boolean) : Output
    }
}
