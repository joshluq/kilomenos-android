package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Use case to handle user sign in process.
 */
class SignInUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val logger: LoggerKit
) : FlowUseCase<SignInUseCase.Input, SignInUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("SignInUseCase", "Executing sign in for: ${input.email}")
        return repository.signIn(input.email, input.password)
            .map { user ->
                logger.i("SignInUseCase", "Sign in successful for: ${user.email}")
                Output.Success(user) as Output
            }
            .onStart { emit(Output.Progress) }
            .catch { throwable ->
                logger.e("SignInUseCase", "Sign in failed: ${throwable.message}")
                val error = (throwable as? KmException)?.error ?: KmError.UnknownError
                emit(Output.Failure(error))
            }
    }

    data class Input(
        val email: String,
        val password: String
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        data class Failure(val error: KmError) : Output
        data class Success(val user: User) : Output
    }
}
