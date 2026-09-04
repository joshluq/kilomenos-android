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
 * Domain interface to handle user sign in process.
 */
interface SignInUseCase : FlowUseCase<SignInUseCase.Input, SignInUseCase.Output> {
    data class Input(
        val email: String,
        val password: String
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Failure(val error: KmError) : Output
        data class Success(val user: User) : Output
    }
}

class SignInUseCaseImpl @Inject constructor(
    private val repository: AuthRepository,
    private val logger: LoggerKit
) : SignInUseCase {

    override fun invoke(input: SignInUseCase.Input): Flow<SignInUseCase.Output> {
        logger.d("SignInUseCase", "Executing sign in for: ${input.email}")
        return repository.signIn(input.email, input.password)
            .map { user ->
                logger.i("SignInUseCase", "Sign in successful for: ${user.email}")
                SignInUseCase.Output.Success(user) as SignInUseCase.Output
            }
            .onStart { emit(SignInUseCase.Output.Progress) }
            .catch { throwable ->
                logger.e("SignInUseCase", "Sign in failed: ${throwable.message}")
                val error = (throwable as? KmException)?.error ?: KmError.UnknownError
                emit(SignInUseCase.Output.Failure(error))
            }
    }
}
