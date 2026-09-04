package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.PreferencesRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Domain interface to evaluate if there is an identity conflict between the current session
 * and the local data stored on the device.
 */
interface EvaluateIdentityConflictUseCase : FlowUseCase<EvaluateIdentityConflictUseCase.Input, EvaluateIdentityConflictUseCase.Output> {
    data class Input(val email: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object NoConflict : Output
        data object SilentCleanup : Output
        data object ShowWarning : Output
    }
}

class EvaluateIdentityConflictUseCaseImpl @Inject constructor(
    private val rentingRepository: RentingRepository,
    private val preferencesRepository: PreferencesRepository,
    private val logger: LoggerKit
) : EvaluateIdentityConflictUseCase {

    override fun invoke(input: EvaluateIdentityConflictUseCase.Input): Flow<EvaluateIdentityConflictUseCase.Output> = flow {
        val currentEmail = input.email.trim().lowercase()

        // One-shot check for conflict
        val dbOwnerId = rentingRepository.getDatabaseOwnerId()
        val hasExistingData = rentingRepository.hasLocalData()
        
        // We use global preferences here because we don't have the userId yet
        val prefs = preferencesRepository.getGlobalPreferences().first()
        val lastEmail = prefs.lastEmail.trim().lowercase()

        logger.d(
            "EvaluateIdentityConflict",
            "Checking conflict - Current: $currentEmail, Last: $lastEmail, DB Owner: $dbOwnerId, HasData: $hasExistingData"
        )

        // Conflict detection logic:
        // 1. If there's an email mismatch and the previous session was authenticated.
        // 2. If there's NO previous email recorded (Free/Local user) but the DB already contains data.
        val isNewUser = currentEmail != lastEmail

        val result = when {
            // Case A: Same user as last time -> No conflict.
            !isNewUser -> EvaluateIdentityConflictUseCase.Output.NoConflict
            
            // Case B: Different user and we have data in the DB -> Warning.
            hasExistingData -> EvaluateIdentityConflictUseCase.Output.ShowWarning
            
            // Case C: Different user but DB is empty -> Silent cleanup (clears prefs/cache).
            else -> EvaluateIdentityConflictUseCase.Output.SilentCleanup
        }

        emit(result)
    }.onStart { emit(EvaluateIdentityConflictUseCase.Output.Progress) }
}
