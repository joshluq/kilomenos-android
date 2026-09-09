package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.AppOverlayState
import es.joshluq.kmsafe.domain.repository.AppOverlayRepository
import es.joshluq.kmsafe.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Domain interface to handle user sign out process.
 */
interface SignOutUseCase : FlowUseCase<SignOutUseCase.Input, SignOutUseCase.Output> {

    data class Input(
        val clearLocalData: Boolean = true,
        val minHoldDurationMs: Long = 1800L
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Failure(val message: String) : Output
        data object Success : Output
    }
}

class SignOutUseCaseImpl @Inject constructor(
    private val repository: AuthRepository,
    private val appOverlayRepository: AppOverlayRepository,
    private val logger: LoggerKit
) : SignOutUseCase {

    override fun invoke(input: SignOutUseCase.Input): Flow<SignOutUseCase.Output> = flow {
        emit(SignOutUseCase.Output.Progress)
        appOverlayRepository.setOverlay(AppOverlayState.LoggingOut)
        try {
            val startTime = System.currentTimeMillis()
            logger.d("SignOutUseCase", "Executing sign out. clearLocalData: ${input.clearLocalData}")
            repository.signOut(input.clearLocalData).first()
            logger.i("SignOutUseCase", "Sign out successful")

            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < input.minHoldDurationMs) {
                delay((input.minHoldDurationMs - elapsed).milliseconds)
            }

            emit(SignOutUseCase.Output.Success)
            // Allow navigation transition to mount Destination.Login before clearing visual shield
            delay(200.milliseconds)
        } finally {
            appOverlayRepository.clearOverlay()
        }
    }.catch {
        logger.e("SignOutUseCase", "Sign out failed", it)
        emit(SignOutUseCase.Output.Failure(it.message ?: "Sign out failed"))
    }
}
