package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.UserPreferences
import es.joshluq.kmsafe.domain.repository.AuthRepository
import es.joshluq.kmsafe.domain.repository.PreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Domain interface to fetch preferences.
 */
interface GetPreferencesUseCase : FlowUseCase<GetPreferencesUseCase.Input, GetPreferencesUseCase.Output> {

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val preferences: UserPreferences) : Output
    }
}

class GetPreferencesUseCaseImpl @Inject constructor(
    private val repository: PreferencesRepository,
    private val authRepository: AuthRepository
) : GetPreferencesUseCase {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: GetPreferencesUseCase.Input): Flow<GetPreferencesUseCase.Output> {
        return authRepository.getCurrentUser().flatMapLatest { user ->
            if (user != null) {
                repository.getPreferences(user.id)
            } else {
                repository.getGlobalPreferences()
            }
        }.map {
            GetPreferencesUseCase.Output.Success(it)
        }
    }
}
