package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Domain interface to handle user sign out process.
 */
interface SignOutUseCase : FlowUseCase<SignOutUseCase.Input, SignOutUseCase.Output> {

    data class Input(val clearLocalData: Boolean = true) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Failure(val message: String) : Output
        data object Success : Output
    }
}

class SignOutUseCaseImpl @Inject constructor(
    private val repository: AuthRepository,
    private val logger: LoggerKit
) : SignOutUseCase {

    override fun invoke(input: SignOutUseCase.Input): Flow<SignOutUseCase.Output> {
        logger.d("SignOutUseCase", "Executing sign out. clearLocalData: ${input.clearLocalData}")
        return repository.signOut(input.clearLocalData)
            .map {
                logger.i("SignOutUseCase", "Sign out successful")
                SignOutUseCase.Output.Success as SignOutUseCase.Output
            }
            .onStart { emit(SignOutUseCase.Output.Progress) }
            .catch {
                logger.e("SignOutUseCase", "Sign out failed", it)
                emit(SignOutUseCase.Output.Failure(it.message ?: "Sign out failed"))
            }
    }
}
