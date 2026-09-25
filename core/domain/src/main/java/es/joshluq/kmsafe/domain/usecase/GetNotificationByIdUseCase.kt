package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * Domain use case to fetch a single notification by id.
 * Conforms to FoundationKit UseCase architecture.
 */
interface GetNotificationByIdUseCase :
    UseCase<GetNotificationByIdUseCase.Input, GetNotificationByIdUseCase.Output> {

    data class Input(val notificationId: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val notification: Notification?) : Output
    }
}

class GetNotificationByIdUseCaseImpl @Inject constructor(
    private val repository: NotificationRepository
) : GetNotificationByIdUseCase {

    override suspend fun invoke(input: GetNotificationByIdUseCase.Input): Result<GetNotificationByIdUseCase.Output> {
        return runCatching {
            val notification = repository.getNotificationById(input.notificationId)
            GetNotificationByIdUseCase.Output.Success(notification)
        }
    }
}
