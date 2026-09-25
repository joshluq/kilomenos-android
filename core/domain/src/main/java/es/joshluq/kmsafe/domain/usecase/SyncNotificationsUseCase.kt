package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.PremiumRequiredException
import es.joshluq.kmsafe.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * Domain usecase to execute notifications bidirectional synchronization.
 */
interface SyncNotificationsUseCase :
    UseCase<SyncNotificationsUseCase.Input, SyncNotificationsUseCase.Output> {

    data object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val syncedCount: Int) : Output
        data object PremiumRequired : Output
        data class Failure(val error: Throwable) : Output
    }
}

class SyncNotificationsUseCaseImpl @Inject constructor(
    private val repository: NotificationRepository
) : SyncNotificationsUseCase {

    override suspend fun invoke(input: SyncNotificationsUseCase.Input): Result<SyncNotificationsUseCase.Output> {
        return runCatching {
            repository.syncPending()
            val fetchResult = repository.fetchRemoteNotifications()
            fetchResult.fold(
                onSuccess = { count ->
                    SyncNotificationsUseCase.Output.Success(count)
                },
                onFailure = { err ->
                    if (err is PremiumRequiredException) {
                        SyncNotificationsUseCase.Output.PremiumRequired
                    } else {
                        SyncNotificationsUseCase.Output.Failure(err)
                    }
                }
            )
        }
    }
}
