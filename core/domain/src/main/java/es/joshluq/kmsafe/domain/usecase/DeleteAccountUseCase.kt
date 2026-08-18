package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Use case to permanently delete the user's account and all associated data.
 * This is a requirement for GDPR compliance and Play Store policy.
 */
class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val logger: LoggerKit
) : FlowUseCase<DeleteAccountUseCase.Input, DeleteAccountUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> = flow {
        logger.d("DeleteAccountUseCase", "Executing account deletion")
        authRepository.deleteAccount().collect {
            emit(Output.Success as Output)
        }
    }
        .onStart { emit(Output.Progress as Output) }
        .catch { error ->
            logger.e("DeleteAccountUseCase", "Error during account deletion", error)
            emit(Output.Failure as Output)
        }

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data object Success : Output
    }
}
