package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.AuthRepository
import es.joshluq.kmsafe.domain.repository.PreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class UpdatePreferencesUseCase @Inject constructor(
    private val repository: PreferencesRepository,
    private val authRepository: AuthRepository
) : FlowUseCase<UpdatePreferencesUseCase.Input, UpdatePreferencesUseCase.Output> {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: Input): Flow<Output> {
        return authRepository.getCurrentUser().flatMapLatest { user ->
            flow {
                input.rememberEmail?.let { repository.setRememberEmail(it) }
                input.lastEmail?.let { repository.saveLastEmail(it) }

                if (user != null) {
                    input.showProjectionBanner?.let { repository.setShowProjectionBanner(user.id, it) }
                    input.lastKnownOverLimit?.let { repository.setLastKnownOverLimit(user.id, it) }
                    input.autoTrackingEnabled?.let { repository.setAutoTrackingEnabled(user.id, it) }
                    input.autoTrackingPromotionDismissed?.let { repository.setAutoTrackingPromotionDismissed(user.id, it) }
                }

                emit(Output.Success as Output)
            }
        }
    }

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
