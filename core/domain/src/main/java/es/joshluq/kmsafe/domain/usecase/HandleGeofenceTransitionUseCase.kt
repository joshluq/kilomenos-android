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
 * Domain interface to handle geofence transition events.
 */
interface HandleGeofenceTransitionUseCase : FlowUseCase<HandleGeofenceTransitionUseCase.Input, HandleGeofenceTransitionUseCase.Output> {
    data class Input(val stationId: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Success : Output
        data object Failure : Output
    }
}

class HandleGeofenceTransitionUseCaseImpl @Inject constructor(
    private val stationRepository: ServiceStationRepository,
    private val notificationService: StationNotificationService,
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
    private val logger: LoggerKit
) : HandleGeofenceTransitionUseCase {

    companion object {
        const val NOTIFICATION_COOLDOWN_MS = 45 * 60 * 1000L // 45 minutes
        val lastNotifiedTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()
    }

    override fun invoke(input: HandleGeofenceTransitionUseCase.Input): Flow<HandleGeofenceTransitionUseCase.Output> = flow {
        logger.d("HandleGeofence", "Transition detected for stationId: ${input.stationId}")

        // 1. Validate Premium Access
        val access = checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.STATION_AUTO_DETECTION)).first()
        if (access !is CheckFeatureAccessUseCase.Output.Success || !access.isGranted) {
            logger.w("HandleGeofence", "User is not Premium or lacks entitlement, ignoring geofence")
            emit(HandleGeofenceTransitionUseCase.Output.Failure)
            return@flow
        }

        // 2. Anti-Spam Cooldown check
        val now = System.currentTimeMillis()
        val lastNotified = lastNotifiedTimestamps[input.stationId] ?: 0L
        if (now - lastNotified < NOTIFICATION_COOLDOWN_MS) {
            logger.d("HandleGeofence", "Station ${input.stationId} notification skipped due to cooldown")
            emit(HandleGeofenceTransitionUseCase.Output.Success)
            return@flow
        }

        // 3. Retrieve station details
        val station = stationRepository.getStationById(input.stationId).first()
        if (station == null) {
            logger.e("HandleGeofence", "Station not found: ${input.stationId}")
            emit(HandleGeofenceTransitionUseCase.Output.Failure)
            return@flow
        }

        // 4. Trigger Proximity Prompt & record timestamp
        logger.i("HandleGeofence", "Triggering proximity prompt for: ${station.name}")
        notificationService.showStationProximityPrompt(station)
        lastNotifiedTimestamps[input.stationId] = now

        emit(HandleGeofenceTransitionUseCase.Output.Success)
    }.catch { e ->
        logger.e("HandleGeofence", "Error handling transition", e)
        emit(HandleGeofenceTransitionUseCase.Output.Failure)
    }
}
