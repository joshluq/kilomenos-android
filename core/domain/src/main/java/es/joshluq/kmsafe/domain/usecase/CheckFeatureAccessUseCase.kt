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
 * Domain interface to check if a specific feature is enabled based on user entitlements.
 */
interface CheckFeatureAccessUseCase : FlowUseCase<CheckFeatureAccessUseCase.Input, CheckFeatureAccessUseCase.Output> {

    data class Input(val feature: Feature) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val isGranted: Boolean) : Output
    }
}

class CheckFeatureAccessUseCaseImpl @Inject constructor(
    private val repository: EntitlementsRepository
) : CheckFeatureAccessUseCase {

    override fun invoke(input: CheckFeatureAccessUseCase.Input): Flow<CheckFeatureAccessUseCase.Output> {
        return repository.observeEntitlements().map { entitlements ->
            CheckFeatureAccessUseCase.Output.Success(entitlements.isFeatureActive(input.feature))
        }
    }
}
