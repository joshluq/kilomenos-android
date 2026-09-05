package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.service.TrackingServiceController
import javax.inject.Inject

/**
 * Domain UseCase to stop the vehicle location tracking service.
 */
interface StopTripTrackingUseCase : UseCase<StopTripTrackingUseCase.Input, StopTripTrackingUseCase.Output> {
    data object Input : UseCaseInput
    data object Output : UseCaseOutput
}

class StopTripTrackingUseCaseImpl @Inject constructor(
    private val controller: TrackingServiceController
) : StopTripTrackingUseCase {

    override suspend fun invoke(input: StopTripTrackingUseCase.Input): Result<StopTripTrackingUseCase.Output> {
        controller.stopTrackingService()
        return Result.success(StopTripTrackingUseCase.Output)
    }
}
