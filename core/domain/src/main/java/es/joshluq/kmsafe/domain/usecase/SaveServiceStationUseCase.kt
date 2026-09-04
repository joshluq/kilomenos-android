package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.util.UUID
import javax.inject.Inject

/**
 * Domain interface to save or update a service station entity.
 */
interface SaveServiceStationUseCase : FlowUseCase<SaveServiceStationUseCase.Input, SaveServiceStationUseCase.Output> {

    data class Input(
        val id: String? = null,
        val name: String,
        val brand: String,
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val isFavorite: Boolean = false,
        val availableEnergies: List<FuelType> = emptyList()
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data class Success(val stationId: String) : Output
    }
}

class SaveServiceStationUseCaseImpl @Inject constructor(
    private val repository: ServiceStationRepository,
    private val logger: LoggerKit
) : SaveServiceStationUseCase {

    override fun invoke(input: SaveServiceStationUseCase.Input): Flow<SaveServiceStationUseCase.Output> {
        logger.d("SaveServiceStation", "UseCase invoked for station name: ${input.name}")
        val station = ServiceStation(
            id = input.id ?: UUID.randomUUID().toString(),
            name = input.name,
            brand = input.brand,
            latitude = input.latitude,
            longitude = input.longitude,
            address = input.address,
            isFavorite = input.isFavorite,
            availableEnergies = input.availableEnergies
        )

        logger.d("SaveServiceStation", "Saving station: ${station.name}")

        return repository.saveStation(station)
            .map { id ->
                SaveServiceStationUseCase.Output.Success(id) as SaveServiceStationUseCase.Output
            }
            .onStart { emit(SaveServiceStationUseCase.Output.Progress) }
            .catch { e ->
                logger.e("SaveServiceStation", "Error saving station", e)
                emit(SaveServiceStationUseCase.Output.Failure)
            }
    }
}
