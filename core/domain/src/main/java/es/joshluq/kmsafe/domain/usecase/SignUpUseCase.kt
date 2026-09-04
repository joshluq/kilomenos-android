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
 * Domain interface to handle user registration process.
 */
interface SignUpUseCase : FlowUseCase<SignUpUseCase.Input, SignUpUseCase.Output> {
    data class Input(
        val email: String,
        val password: String,
        val name: String?
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Failure(val error: KmError) : Output
        data class Success(val user: User) : Output
    }
}

class SignUpUseCaseImpl @Inject constructor(
    private val repository: AuthRepository,
    private val logger: LoggerKit
) : SignUpUseCase {

    override fun invoke(input: SignUpUseCase.Input): Flow<SignUpUseCase.Output> {
        logger.d("SignUpUseCase", "Executing sign up for: ${input.email}")
        return repository.signUp(input.email, input.password, input.name)
            .map { user ->
                logger.i("SignUpUseCase", "Sign up request successful for: ${user.email}")
                SignUpUseCase.Output.Success(user) as SignUpUseCase.Output
            }
            .onStart { emit(SignUpUseCase.Output.Progress) }
            .catch { throwable ->
                logger.e("SignUpUseCase", "Sign up failed: ${throwable.message}")
                val error = (throwable as? KmException)?.error ?: KmError.UnknownError
                emit(SignUpUseCase.Output.Failure(error))
            }
    }
}
