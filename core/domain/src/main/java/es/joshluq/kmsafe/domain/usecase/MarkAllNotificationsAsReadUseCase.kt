package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * Domain use case to mark all active notifications as read.
 * Conforms to FoundationKit UseCase architecture.
 */
interface MarkAllNotificationsAsReadUseCase :
    UseCase<MarkAllNotificationsAsReadUseCase.Input, MarkAllNotificationsAsReadUseCase.Output> {

    data object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Success : Output
    }
}

class MarkAllNotificationsAsReadUseCaseImpl @Inject constructor(
    private val repository: NotificationRepository
) : MarkAllNotificationsAsReadUseCase {

    override suspend fun invoke(input: MarkAllNotificationsAsReadUseCase.Input): Result<MarkAllNotificationsAsReadUseCase.Output> {
        return runCatching {
            repository.markAllAsRead()
            MarkAllNotificationsAsReadUseCase.Output.Success
        }
    }
}
