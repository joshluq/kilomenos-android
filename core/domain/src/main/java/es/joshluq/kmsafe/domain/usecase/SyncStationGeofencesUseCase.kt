package es.joshluq.kmsafe.core.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import es.joshluq.kmsafe.core.domain.service.GeofenceService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Domain interface to synchronize local favorite service stations with the system geofences.
 */
interface SyncStationGeofencesUseCase : FlowUseCase<SyncStationGeofencesUseCase.Input, SyncStationGeofencesUseCase.Output> {

    data object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data object Success : Output
    }
}

class SyncStationGeofencesUseCaseImpl @Inject constructor(
    private val stationRepository: ServiceStationRepository,
    private val geofenceService: GeofenceService,
    private val logger: LoggerKit
) : SyncStationGeofencesUseCase {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun invoke(input: SyncStationGeofencesUseCase.Input): Flow<SyncStationGeofencesUseCase.Output> {
        logger.d("SyncStationGeofences", "Starting synchronization")
        
        return stationRepository.getFavoriteStations()
            .flatMapLatest { favorites ->
                logger.d("SyncStationGeofences", "Registering ${favorites.size} favorite stations as geofences")
                geofenceService.registerStationGeofences(favorites)
            }
            .map {
                SyncStationGeofencesUseCase.Output.Success as SyncStationGeofencesUseCase.Output
            }
            .onStart { emit(SyncStationGeofencesUseCase.Output.Progress) }
            .catch { e ->
                logger.e("SyncStationGeofences", "Error synchronizing geofences", e)
                emit(SyncStationGeofencesUseCase.Output.Failure)
            }
    }
}
