package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case to check if the current user has a Premium subscription.
 */
class IsUserPremiumUseCase @Inject constructor(
    private val repository: AuthRepository
) : FlowUseCase<IsUserPremiumUseCase.Input, IsUserPremiumUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        return repository.getCurrentUser().map { user ->
            val isPremium = user?.subscriptionLevel == SubscriptionLevel.PREMIUM
            Output.Success(isPremium)
        }
    }

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val isPremium: Boolean) : Output
    }
}
