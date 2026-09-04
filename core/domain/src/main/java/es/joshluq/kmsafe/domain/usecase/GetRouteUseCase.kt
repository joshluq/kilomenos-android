package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.TripRoute
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Domain interface to fetch the trip route associated with an odometer record.
 */
interface GetRouteUseCase : FlowUseCase<GetRouteUseCase.Input, GetRouteUseCase.Output> {

    data class Input(val recordId: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Failure(val message: String) : Output
        data class Success(val route: TripRoute) : Output
    }
}

class GetRouteUseCaseImpl @Inject constructor(
    private val repository: HistoryRepository,
    private val logger: LoggerKit
) : GetRouteUseCase {

    override fun invoke(input: GetRouteUseCase.Input): Flow<GetRouteUseCase.Output> {
        logger.d("GetRouteUseCase", "Fetching route for record: ${input.recordId}")
        return repository.getRoute(input.recordId)
            .map { route ->
                if (route == null) {
                    GetRouteUseCase.Output.Failure("No route found for this record")
                } else {
                    GetRouteUseCase.Output.Success(route)
                }
            }
            .onStart { emit(GetRouteUseCase.Output.Progress) }
            .catch {
                logger.e("GetRouteUseCase", "Error fetching route", it)
                emit(GetRouteUseCase.Output.Failure(it.message ?: "Unknown error"))
            }
    }
}
