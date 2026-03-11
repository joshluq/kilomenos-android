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
 * Use case to evaluate if there is an identity conflict between the current session
 * and the local data stored on the device.
 */
class EvaluateIdentityConflictUseCase @Inject constructor(
    private val rentingRepository: RentingRepository,
    private val preferencesRepository: PreferencesRepository,
    private val logger: LoggerKit
) : FlowUseCase<EvaluateIdentityConflictUseCase.Input, EvaluateIdentityConflictUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> = flow {
        val currentEmail = input.email.trim().lowercase()
        
        // One-shot check for conflict
        val dbOwnerId = rentingRepository.getDatabaseOwnerId()
        // We use global preferences here because we don't have the userId yet
        val prefs = preferencesRepository.getGlobalPreferences().first()
        val lastEmail = prefs.lastEmail.trim().lowercase()

        logger.d("EvaluateIdentityConflict", "Checking conflict - Current: $currentEmail, Last: $lastEmail, DB Owner: $dbOwnerId")

        val isNewUser = currentEmail != lastEmail && lastEmail.isNotEmpty()
        val hasDataToProtect = dbOwnerId != null

        val result = when {
            !isNewUser -> Output.NoConflict
            hasDataToProtect -> Output.ShowWarning
            else -> Output.SilentCleanup
        }
        
        emit(result)
    }.onStart { emit(Output.Progress) }

    data class Input(val email: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object NoConflict : Output
        data object SilentCleanup : Output
        data object ShowWarning : Output
    }
}
