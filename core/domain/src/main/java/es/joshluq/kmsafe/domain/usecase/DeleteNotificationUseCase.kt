package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * Domain use case to delete a notification locally and remotely.
 */
interface DeleteNotificationUseCase :
    UseCase<DeleteNotificationUseCase.Input, DeleteNotificationUseCase.Output> {

    data class Input(val notificationId: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Success : Output
    }
}

class DeleteNotificationUseCaseImpl @Inject constructor(
    private val repository: NotificationRepository
) : DeleteNotificationUseCase {

    override suspend fun invoke(input: DeleteNotificationUseCase.Input): Result<DeleteNotificationUseCase.Output> {
        return runCatching {
            repository.delete(input.notificationId)
            DeleteNotificationUseCase.Output.Success
        }
    }
}
