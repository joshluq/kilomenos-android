package es.joshluq.kmsafe.domain.usecase

import es.joshluq.authkit.session.model.SessionState
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Use case to check the current session status.
 */
class CheckSessionUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val logger: LoggerKit
) : FlowUseCase<CheckSessionUseCase.Input, CheckSessionUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("CheckSessionUseCase", "Checking session state")
        return repository.getSessionState()
            .map { state ->
                when (state) {
                    SessionState.Active, SessionState.ExpiringSoon -> Output.ActiveSession
                    SessionState.Idle -> Output.IdleSession
                    SessionState.Initializing -> Output.Progress
                }
            }
            .onEach { logger.i("CheckSessionUseCase", "Session result: $it") }
    }

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object ActiveSession : Output
        data object IdleSession : Output
    }
}
