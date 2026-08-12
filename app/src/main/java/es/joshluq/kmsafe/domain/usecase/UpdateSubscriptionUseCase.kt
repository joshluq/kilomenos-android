package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Use case to update the user's subscription level.
 */
class UpdateSubscriptionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val logger: LoggerKit
) : FlowUseCase<UpdateSubscriptionUseCase.Input, UpdateSubscriptionUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("UpdateSubscriptionUseCase", "Updating subscription to ${input.level}")
        return authRepository.updateSubscription(input.level)
            .map { user ->
                logger.i("UpdateSubscriptionUseCase", "Subscription updated successfully for user ${user.id}")
                Output.Success(user) as Output
            }
            .onStart { emit(Output.Progress) }
            .catch { error ->
                logger.e("UpdateSubscriptionUseCase", "Error updating subscription", error)
                emit(Output.Failure)
            }
    }

    data class Input(val level: SubscriptionLevel) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data class Success(val user: User) : Output
    }
}
