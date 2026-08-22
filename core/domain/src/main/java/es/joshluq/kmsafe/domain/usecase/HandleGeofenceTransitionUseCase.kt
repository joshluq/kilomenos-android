package es.joshluq.kmsafe.core.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import es.joshluq.kmsafe.core.domain.service.StationNotificationService
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case to handle geofence transition events (e.g. entering a station area).
 * Validates Premium access and triggers the appropriate notification prompt.
 */
class HandleGeofenceTransitionUseCase @Inject constructor(
    private val stationRepository: ServiceStationRepository,
    private val notificationService: StationNotificationService,
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
    private val logger: LoggerKit
) : FlowUseCase<HandleGeofenceTransitionUseCase.Input, HandleGeofenceTransitionUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> = flow {
        logger.d("HandleGeofence", "Transition detected for stationId: ${input.stationId}")

        // 1. Validate Premium Access (Fase 3 is Premium)
        val access = checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.AUTO_TRACKING)).first()
        if (access !is CheckFeatureAccessUseCase.Output.Success || !access.isGranted) {
            logger.w("HandleGeofence", "User is not Premium, ignoring geofence")
            emit(Output.Failure)
            return@flow
        }

        // 2. Retrieve station details
        val station = stationRepository.getStationById(input.stationId).first()
        if (station == null) {
            logger.e("HandleGeofence", "Station not found: ${input.stationId}")
            emit(Output.Failure)
            return@flow
        }

        // 3. Trigger Proximity Prompt
        logger.i("HandleGeofence", "Triggering proximity prompt for: ${station.name}")
        notificationService.showStationProximityPrompt(station)
        
        emit(Output.Success)
    }.catch { e ->
        logger.e("HandleGeofence", "Error handling transition", e)
        emit(Output.Failure)
    }

    data class Input(val stationId: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Success : Output
        data object Failure : Output
    }
}
