package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Use case to retrieve only favorite service stations.
 */
class GetFavoriteServiceStationsUseCase @Inject constructor(
    private val repository: ServiceStationRepository,
    private val logger: LoggerKit
) : FlowUseCase<GetFavoriteServiceStationsUseCase.Input, GetFavoriteServiceStationsUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("GetFavoriteStations", "Fetching favorite stations")
        return repository.getFavoriteStations()
            .map { stations ->
                Output.Success(stations) as Output
            }
            .onStart { emit(Output.Progress) }
            .catch { e ->
                logger.e("GetFavoriteStations", "Error fetching favorites", e)
                emit(Output.Failure)
            }
    }

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data class Success(val stations: List<ServiceStation>) : Output
    }
}
