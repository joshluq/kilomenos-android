package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Domain interface to observe the currently authenticated user.
 */
interface GetCurrentUserUseCase : FlowUseCase<GetCurrentUserUseCase.Input, GetCurrentUserUseCase.Output> {

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val user: User?) : Output
    }
}

class GetCurrentUserUseCaseImpl @Inject constructor(
    private val repository: AuthRepository,
    private val logger: LoggerKit
) : GetCurrentUserUseCase {

    override fun invoke(input: GetCurrentUserUseCase.Input): Flow<GetCurrentUserUseCase.Output> {
        logger.d("GetCurrentUserUseCase", "Observing current user")
        return repository.getCurrentUser().map { user ->
            GetCurrentUserUseCase.Output.Success(user)
        }.onEach {
            logger.d("GetCurrentUserUseCase", "User updated: ${it.user?.email ?: "None"}")
        }
    }
}
