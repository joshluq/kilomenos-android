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
 * Domain interface to permanently delete the user's account and all associated data.
 */
interface DeleteAccountUseCase : FlowUseCase<DeleteAccountUseCase.Input, DeleteAccountUseCase.Output> {

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data object Success : Output
    }
}

class DeleteAccountUseCaseImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val logger: LoggerKit
) : DeleteAccountUseCase {

    override fun invoke(input: DeleteAccountUseCase.Input): Flow<DeleteAccountUseCase.Output> = flow {
        logger.d("DeleteAccountUseCase", "Executing account deletion")
        authRepository.deleteAccount().collect {
            emit(DeleteAccountUseCase.Output.Success as DeleteAccountUseCase.Output)
        }
    }
        .onStart { emit(DeleteAccountUseCase.Output.Progress as DeleteAccountUseCase.Output) }
        .catch { error ->
            logger.e("DeleteAccountUseCase", "Error during account deletion", error)
            emit(DeleteAccountUseCase.Output.Failure as DeleteAccountUseCase.Output)
        }
}
