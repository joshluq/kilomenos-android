package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.service.TrackingServiceController
import javax.inject.Inject

/**
 * Domain UseCase to dismiss the vehicle trip finished notification.
 */
interface DismissTripNotificationUseCase : UseCase<DismissTripNotificationUseCase.Input, DismissTripNotificationUseCase.Output> {
    data object Input : UseCaseInput
    data object Output : UseCaseOutput
}

class DismissTripNotificationUseCaseImpl @Inject constructor(
    private val controller: TrackingServiceController
) : DismissTripNotificationUseCase {

    override suspend fun invoke(input: DismissTripNotificationUseCase.Input): Result<DismissTripNotificationUseCase.Output> {
        controller.dismissTripFinishedNotification()
        return Result.success(DismissTripNotificationUseCase.Output)
    }
}
