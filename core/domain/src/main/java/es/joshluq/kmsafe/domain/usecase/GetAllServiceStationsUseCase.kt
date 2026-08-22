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
 * Use case to retrieve all recorded service stations from local storage.
 */
class GetAllServiceStationsUseCase @Inject constructor(
    private val repository: ServiceStationRepository,
    private val logger: LoggerKit
) : FlowUseCase<GetAllServiceStationsUseCase.Input, GetAllServiceStationsUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("GetAllServiceStations", "Fetching all stations")
        return repository.getAllStations()
            .map { stations ->
                if (stations.isEmpty()) {
                    Output.Empty
                } else {
                    Output.Success(stations)
                }
            }
            .onStart { emit(Output.Progress) }
            .catch { e ->
                logger.e("GetAllServiceStations", "Error fetching stations", e)
                emit(Output.Failure)
            }
    }

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data object Empty : Output
        data class Success(val stations: List<ServiceStation>) : Output
    }
}
