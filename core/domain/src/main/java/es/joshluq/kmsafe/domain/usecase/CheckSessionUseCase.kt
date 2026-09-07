package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.AuthSessionState
import es.joshluq.kmsafe.domain.repository.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Domain interface to check the current session status.
 */
interface CheckSessionUseCase : FlowUseCase<CheckSessionUseCase.Input, CheckSessionUseCase.Output> {
    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object ActiveSession : Output
        data object InconsistentSession : Output
        data object IdleSession : Output
    }
}

class CheckSessionUseCaseImpl @Inject constructor(
    private val repository: AuthRepository,
    private val logger: LoggerKit
) : CheckSessionUseCase {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: CheckSessionUseCase.Input): Flow<CheckSessionUseCase.Output> {
        logger.d("CheckSessionUseCase", "Checking session state and integrity")
        return repository.getSessionState().flatMapLatest { state ->
            logger.d("CheckSessionUseCase", "Session state changed: $state")
            when (state) {
                AuthSessionState.Active, AuthSessionState.ExpiringSoon -> {
                    repository.getCurrentUser().map { user ->
                        logger.d("CheckSessionUseCase", "Evaluating active session user: $user")
                        if (user != null) {
                            logger.i("CheckSessionUseCase", "Active session confirmed for user: ${user.email} (${user.id})")
                            CheckSessionUseCase.Output.ActiveSession
                        } else {
                            logger.e("CheckSessionUseCase", "Integrity Failure: Session active ($state) but User is NULL")
                            CheckSessionUseCase.Output.InconsistentSession
                        }
                    }
                }
                AuthSessionState.Idle -> flowOf(CheckSessionUseCase.Output.IdleSession)
                AuthSessionState.Initializing -> flowOf(CheckSessionUseCase.Output.Progress)
            }
        }.onEach { logger.i("CheckSessionUseCase", "Session check result: $it") }
    }
}
