package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationStatus
import es.joshluq.kmsafe.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * Domain UseCase that validates notification metadata before publishing.
 * If the notification has already been marked as read, publication is skipped to prevent resurrecting read alerts.
 */
interface PublishNotificationIfUnreadUseCase :
    UseCase<PublishNotificationIfUnreadUseCase.Input, PublishNotificationIfUnreadUseCase.Output> {

    data class Input(val notification: Notification) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Published : Output
        data object SkippedAlreadyRead : Output
    }
}

class PublishNotificationIfUnreadUseCaseImpl @Inject constructor(
    private val repository: NotificationRepository,
    private val publishNotificationUseCase: PublishNotificationUseCase
) : PublishNotificationIfUnreadUseCase {

    override suspend fun invoke(input: PublishNotificationIfUnreadUseCase.Input): Result<PublishNotificationIfUnreadUseCase.Output> {
        val semanticKey = input.notification.data?.get("projection_key") as? String
            ?: input.notification.data?.get("deduplication_key") as? String

        if (semanticKey != null) {
            val existing = repository.getNotificationBySemanticKey(semanticKey)
            if (existing != null && (existing.isRead || existing.status == NotificationStatus.READ)) {
                return Result.success(PublishNotificationIfUnreadUseCase.Output.SkippedAlreadyRead)
            }
        }

        publishNotificationUseCase(PublishNotificationUseCase.Input(input.notification))
        return Result.success(PublishNotificationIfUnreadUseCase.Output.Published)
    }
}
