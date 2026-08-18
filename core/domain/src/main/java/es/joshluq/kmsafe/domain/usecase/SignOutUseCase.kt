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
 * Use case to handle user sign out process.
 */
class SignOutUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val logger: LoggerKit
) : FlowUseCase<SignOutUseCase.Input, SignOutUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("SignOutUseCase", "Executing sign out. clearLocalData: ${input.clearLocalData}")
        return repository.signOut(input.clearLocalData)
            .map {
                logger.i("SignOutUseCase", "Sign out successful")
                Output.Success as Output
            }
            .onStart { emit(Output.Progress) }
            .catch {
                logger.e("SignOutUseCase", "Sign out failed", it)
                emit(Output.Failure(it.message ?: "Sign out failed"))
            }
    }

    data class Input(val clearLocalData: Boolean = true) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Failure(val message: String) : Output
        data object Success : Output
    }
}
