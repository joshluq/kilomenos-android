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
 * Use case to toggle the favorite status of a service station.
 */
class SetFavoriteStationUseCase @Inject constructor(
    private val repository: ServiceStationRepository,
    private val logger: LoggerKit
) : FlowUseCase<SetFavoriteStationUseCase.Input, SetFavoriteStationUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("SetFavoriteStation", "Setting favorite=${input.isFavorite} for station: ${input.stationId}")
        return repository.setFavorite(input.stationId, input.isFavorite)
            .map {
                Output.Success as Output
            }
            .onStart { emit(Output.Progress) }
            .catch { e ->
                logger.e("SetFavoriteStation", "Error setting favorite", e)
                emit(Output.Failure)
            }
    }

    data class Input(val stationId: String, val isFavorite: Boolean) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data object Success : Output
    }
}
