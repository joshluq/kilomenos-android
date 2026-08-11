package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Use case to observe the current trip tracking status and progress.
 */
class ObserveTrackingStateUseCase @Inject constructor(
    private val repository: TrackingRepository
) : FlowUseCase<ObserveTrackingStateUseCase.Input, ObserveTrackingStateUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        return combine(
            repository.isTracking,
            repository.currentDistanceMeters,
            repository.startTime,
            repository.currentRoutePolyline,
            repository.pointCount
        ) { isTracking, distance, startTime, polyline, points ->
            Output.Success(isTracking, distance, startTime, polyline, points)
        }
    }

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(
            val isTracking: Boolean,
            val trackedDistance: Double,
            val startTime: Long?,
            val encodedPolyline: String?,
            val pointCount: Int
        ) : Output
    }
}
