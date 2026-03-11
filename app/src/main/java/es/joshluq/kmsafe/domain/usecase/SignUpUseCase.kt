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
 * Use case to handle user registration process.
 */
class SignUpUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val logger: LoggerKit
) : FlowUseCase<SignUpUseCase.Input, SignUpUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("SignUpUseCase", "Executing sign up for: ${input.email}")
        return repository.signUp(input.email, input.password, input.name)
            .map { user ->
                logger.i("SignUpUseCase", "Sign up request successful for: ${user.email}")
                Output.Success(user) as Output
            }
            .onStart { emit(Output.Progress) }
            .catch { throwable ->
                logger.e("SignUpUseCase", "Sign up failed: ${throwable.message}")
                val error = (throwable as? KmException)?.error ?: KmError.UnknownError
                emit(Output.Failure(error))
            }
    }

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
