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
 * Domain interface to update the user's subscription level.
 */
interface UpdateSubscriptionUseCase : FlowUseCase<UpdateSubscriptionUseCase.Input, UpdateSubscriptionUseCase.Output> {

    data class Input(val level: SubscriptionLevel) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data class Success(val user: User) : Output
    }
}

class UpdateSubscriptionUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val logger: LoggerKit
) : UpdateSubscriptionUseCase {

    override fun invoke(input: UpdateSubscriptionUseCase.Input): Flow<UpdateSubscriptionUseCase.Output> {
        logger.d("UpdateSubscriptionUseCase", "Updating subscription to ${input.level}")
        return authRepository.updateSubscription(input.level)
            .map { user ->
                logger.i("UpdateSubscriptionUseCase", "Subscription updated successfully for user ${user.id}")
                UpdateSubscriptionUseCase.Output.Success(user) as UpdateSubscriptionUseCase.Output
            }
            .onStart { emit(UpdateSubscriptionUseCase.Output.Progress) }
            .catch { error ->
                logger.e("UpdateSubscriptionUseCase", "Error updating subscription", error)
                emit(UpdateSubscriptionUseCase.Output.Failure)
            }
    }
}
