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
 * Domain interface to retrieve all recorded service stations from local storage.
 */
interface GetAllServiceStationsUseCase : FlowUseCase<GetAllServiceStationsUseCase.Input, GetAllServiceStationsUseCase.Output> {

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data object Empty : Output
        data class Success(val stations: List<ServiceStation>) : Output
    }
}

class GetAllServiceStationsUseCaseImpl @Inject constructor(
    private val repository: ServiceStationRepository,
    private val logger: LoggerKit
) : GetAllServiceStationsUseCase {

    override fun invoke(input: GetAllServiceStationsUseCase.Input): Flow<GetAllServiceStationsUseCase.Output> {
        logger.d("GetAllServiceStations", "Fetching all stations")
        return repository.getAllStations()
            .map { stations ->
                if (stations.isEmpty()) {
                    GetAllServiceStationsUseCase.Output.Empty
                } else {
                    GetAllServiceStationsUseCase.Output.Success(stations)
                }
            }
            .onStart { emit(GetAllServiceStationsUseCase.Output.Progress) }
            .catch { e ->
                logger.e("GetAllServiceStations", "Error fetching stations", e)
                emit(GetAllServiceStationsUseCase.Output.Failure)
            }
    }
}
