package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Domain interface to permanently delete a service station entry.
 */
interface DeleteServiceStationUseCase : FlowUseCase<DeleteServiceStationUseCase.Input, DeleteServiceStationUseCase.Output> {

    data class Input(val stationId: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data object Success : Output
    }
}

class DeleteServiceStationUseCaseImpl @Inject constructor(
    private val repository: ServiceStationRepository,
    private val logger: LoggerKit
) : DeleteServiceStationUseCase {

    override fun invoke(input: DeleteServiceStationUseCase.Input): Flow<DeleteServiceStationUseCase.Output> {
        logger.d("DeleteServiceStation", "Deleting station: ${input.stationId}")
        return repository.deleteStation(input.stationId)
            .map {
                DeleteServiceStationUseCase.Output.Success as DeleteServiceStationUseCase.Output
            }
            .onStart { emit(DeleteServiceStationUseCase.Output.Progress) }
            .catch { e ->
                logger.e("DeleteServiceStation", "Error deleting station", e)
                emit(DeleteServiceStationUseCase.Output.Failure)
            }
    }
}
