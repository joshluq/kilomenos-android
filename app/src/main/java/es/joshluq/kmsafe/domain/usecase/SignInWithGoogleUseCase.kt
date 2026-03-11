package es.joshluq.kmsafe.domain.usecase

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
 * Use case for signing in with Google OAuth.
 */
class SignInWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository
) : FlowUseCase<SignInWithGoogleUseCase.Input, SignInWithGoogleUseCase.Output> {

    data class Input(val idToken: String) : UseCaseInput
    
    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Success(val user: User) : Output
        data class Failure(val error: KmError) : Output
    }

    override fun invoke(input: Input): Flow<Output> {
        return repository.signInWithGoogle(input.idToken)
            .map { user -> Output.Success(user) as Output }
            .onStart { emit(Output.Progress) }
            .catch { throwable ->
                val error = (throwable as? KmException)?.error ?: KmError.UnknownError
                emit(Output.Failure(error))
            }
    }
}
