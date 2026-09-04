package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.AuthRepository
import es.joshluq.kmsafe.domain.repository.PreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Domain interface to update user preferences.
 */
interface UpdatePreferencesUseCase : FlowUseCase<UpdatePreferencesUseCase.Input, UpdatePreferencesUseCase.Output> {

    data class Input(
        val rememberEmail: Boolean? = null,
        val lastEmail: String? = null,
        val showProjectionBanner: Boolean? = null,
        val lastKnownOverLimit: Boolean? = null,
        val autoTrackingEnabled: Boolean? = null,
        val autoTrackingPromotionDismissed: Boolean? = null
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Success : Output
    }
}

class UpdatePreferencesUseCaseImpl @Inject constructor(
    private val repository: PreferencesRepository,
    private val authRepository: AuthRepository,
    private val logger: LoggerKit
) : UpdatePreferencesUseCase {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: UpdatePreferencesUseCase.Input): Flow<UpdatePreferencesUseCase.Output> {
        return authRepository.getCurrentUser().flatMapLatest { user ->
            flow {
                logger.d("UpdatePreferencesUseCase", "Updating preferences for user: ${user?.id ?: "GLOBAL"}")
                input.rememberEmail?.let { repository.setRememberEmail(it) }
                input.lastEmail?.let { repository.saveLastEmail(it) }

                if (user != null) {
                    input.showProjectionBanner?.let { repository.setShowProjectionBanner(user.id, it) }
                    input.lastKnownOverLimit?.let { repository.setLastKnownOverLimit(user.id, it) }
                    input.autoTrackingEnabled?.let {
                        logger.i("UpdatePreferencesUseCase", "Persisting autoTrackingEnabled=$it for ${user.id}")
                        repository.setAutoTrackingEnabled(user.id, it)
                    }
                    input.autoTrackingPromotionDismissed?.let {
                        repository.setAutoTrackingPromotionDismissed(
                            user.id,
                            it
                        )
                    }
                } else if (input.autoTrackingEnabled != null) {
                    logger.w("UpdatePreferencesUseCase", "CRITICAL: Attempted to save per-user preference but USER IS NULL")
                }

                emit(UpdatePreferencesUseCase.Output.Success as UpdatePreferencesUseCase.Output)
            }
        }
    }
}
