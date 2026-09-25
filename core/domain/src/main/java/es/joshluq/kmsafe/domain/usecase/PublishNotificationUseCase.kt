package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * Domain use case to publish a notification locally or from background event.
 * Conforms to FoundationKit UseCase architecture.
 */
interface PublishNotificationUseCase :
    UseCase<PublishNotificationUseCase.Input, PublishNotificationUseCase.Output> {

    data class Input(val notification: Notification) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Success : Output
    }
}

class PublishNotificationUseCaseImpl @Inject constructor(
    private val repository: NotificationRepository
) : PublishNotificationUseCase {

    override suspend fun invoke(input: PublishNotificationUseCase.Input): Result<PublishNotificationUseCase.Output> {
        return runCatching {
            repository.insertOrUpdate(input.notification)
            PublishNotificationUseCase.Output.Success
        }
    }
}
