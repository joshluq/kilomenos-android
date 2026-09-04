package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.AuthSessionState
import es.joshluq.kmsafe.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    override fun invoke(input: CheckSessionUseCase.Input): Flow<CheckSessionUseCase.Output> {
        logger.d("CheckSessionUseCase", "Checking session state and integrity")
        return combine(
            repository.getSessionState(),
            repository.getCurrentUser()
        ) { state, user ->
            when (state) {
                AuthSessionState.Active, AuthSessionState.ExpiringSoon -> {
                    if (user != null) {
                        CheckSessionUseCase.Output.ActiveSession
                    } else {
                        // Integrity failure: Session is active in AuthKit but user data is null/unreadable
                        logger.e("CheckSessionUseCase", "Integrity Failure: Session active but User is NULL")
                        CheckSessionUseCase.Output.InconsistentSession
                    }
                }
                AuthSessionState.Idle -> CheckSessionUseCase.Output.IdleSession
                AuthSessionState.Initializing -> CheckSessionUseCase.Output.Progress
            }
        }.onEach { logger.i("CheckSessionUseCase", "Session check result: $it") }
    }
}
