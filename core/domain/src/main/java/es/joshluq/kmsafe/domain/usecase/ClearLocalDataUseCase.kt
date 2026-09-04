package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.PreferencesRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Domain interface to clear all local data.
 */
interface ClearLocalDataUseCase : FlowUseCase<ClearLocalDataUseCase.Input, ClearLocalDataUseCase.Output> {
    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        object Success : Output
    }
}

class ClearLocalDataUseCaseImpl @Inject constructor(
    private val rentingRepository: RentingRepository,
    private val preferencesRepository: PreferencesRepository,
    private val logger: LoggerKit
) : ClearLocalDataUseCase {

    override fun invoke(input: ClearLocalDataUseCase.Input): Flow<ClearLocalDataUseCase.Output> = flow {
        logger.d("ClearLocalDataUseCase", "Clearing all local data (DB and Preferences)")

        // Clear DB
        rentingRepository.clearAllLocalData().collect { }

        // Clear Preferences (Identity)
        preferencesRepository.clearPreferences()

        emit(ClearLocalDataUseCase.Output.Success as ClearLocalDataUseCase.Output)
    }.onStart { emit(ClearLocalDataUseCase.Output.Progress) }
        .catch {
            logger.e("ClearLocalDataUseCase", "Failed to clear data", it)
            emit(ClearLocalDataUseCase.Output.Failure)
        }
}
