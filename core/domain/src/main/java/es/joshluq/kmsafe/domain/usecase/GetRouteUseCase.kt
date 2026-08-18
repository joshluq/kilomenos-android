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
 * Use case to fetch the trip route associated with an odometer record.
 */
class GetRouteUseCase @Inject constructor(
    private val repository: HistoryRepository,
    private val logger: LoggerKit
) : FlowUseCase<GetRouteUseCase.Input, GetRouteUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("GetRouteUseCase", "Fetching route for record: ${input.recordId}")
        return repository.getRoute(input.recordId)
            .map { route ->
                if (route == null) {
                    Output.Failure("No route found for this record")
                } else {
                    Output.Success(route)
                }
            }
            .onStart { emit(Output.Progress) }
            .catch {
                logger.e("GetRouteUseCase", "Error fetching route", it)
                emit(Output.Failure(it.message ?: "Unknown error"))
            }
    }

    data class Input(val recordId: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Failure(val message: String) : Output
        data class Success(val route: TripRoute) : Output
    }
}
