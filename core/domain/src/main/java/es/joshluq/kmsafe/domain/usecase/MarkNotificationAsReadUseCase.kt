package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * Domain use case to mark a single notification as read.
 * Conforms to FoundationKit UseCase architecture.
 */
interface MarkNotificationAsReadUseCase :
    UseCase<MarkNotificationAsReadUseCase.Input, MarkNotificationAsReadUseCase.Output> {

    data class Input(val notificationId: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Success : Output
    }
}

class MarkNotificationAsReadUseCaseImpl @Inject constructor(
    private val repository: NotificationRepository
) : MarkNotificationAsReadUseCase {

    override suspend fun invoke(input: MarkNotificationAsReadUseCase.Input): Result<MarkNotificationAsReadUseCase.Output> {
        return runCatching {
            repository.markAsRead(input.notificationId)
            MarkNotificationAsReadUseCase.Output.Success
        }
    }
}
