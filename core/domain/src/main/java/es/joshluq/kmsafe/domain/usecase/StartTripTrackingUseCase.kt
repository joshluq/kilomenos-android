package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.service.TrackingServiceController
import javax.inject.Inject

/**
 * Domain UseCase to start the vehicle location tracking service.
 */
interface StartTripTrackingUseCase : UseCase<StartTripTrackingUseCase.Input, StartTripTrackingUseCase.Output> {
    data object Input : UseCaseInput
    data object Output : UseCaseOutput
}

class StartTripTrackingUseCaseImpl @Inject constructor(
    private val controller: TrackingServiceController
) : StartTripTrackingUseCase {

    override suspend fun invoke(input: StartTripTrackingUseCase.Input): Result<StartTripTrackingUseCase.Output> {
        controller.startTrackingService()
        return Result.success(StartTripTrackingUseCase.Output)
    }
}
