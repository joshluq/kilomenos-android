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
 * Use case to permanently delete a service station entry.
 */
class DeleteServiceStationUseCase @Inject constructor(
    private val repository: ServiceStationRepository,
    private val logger: LoggerKit
) : FlowUseCase<DeleteServiceStationUseCase.Input, DeleteServiceStationUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("DeleteServiceStation", "Deleting station: ${input.stationId}")
        return repository.deleteStation(input.stationId)
            .map {
                Output.Success as Output
            }
            .onStart { emit(Output.Progress) }
            .catch { e ->
                logger.e("DeleteServiceStation", "Error deleting station", e)
                emit(Output.Failure)
            }
    }

    data class Input(val stationId: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data object Success : Output
    }
}
