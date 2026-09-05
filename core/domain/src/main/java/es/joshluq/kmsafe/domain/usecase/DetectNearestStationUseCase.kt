package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Domain UseCase to detect the nearest service station or EV charging point
 * within a geographical radius (default 120 meters) when the vehicle stops.
 *
 * Governed strictly by the [Feature.STATION_AUTO_DETECTION] entitlement.
 */
interface DetectNearestStationUseCase :
    FlowUseCase<DetectNearestStationUseCase.Input, DetectNearestStationUseCase.Output> {

    data class Input(
        val latitude: Double,
        val longitude: Double,
        val maxRadiusMeters: Double = 120.0
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Found(
            val station: ServiceStation,
            val distanceMeters: Double
        ) : Output

        data object NotFound : Output
        data object AccessDenied : Output
    }
}

class DetectNearestStationUseCaseImpl @Inject constructor(
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
    private val stationRepository: ServiceStationRepository,
    private val logger: LoggerKit
) : DetectNearestStationUseCase {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: DetectNearestStationUseCase.Input): Flow<DetectNearestStationUseCase.Output> {
        logger.d(
            "DetectNearestStationUseCase",
            "Checking station arrival at (${input.latitude}, ${input.longitude}) with radius ${input.maxRadiusMeters}m"
        )

        return checkFeatureAccessUseCase(
            CheckFeatureAccessUseCase.Input(Feature.STATION_AUTO_DETECTION)
        ).flatMapLatest { accessOutput ->
            val isGranted = (accessOutput is CheckFeatureAccessUseCase.Output.Success) && accessOutput.isGranted
            if (!isGranted) {
                logger.d("DetectNearestStationUseCase", "Access denied for STATION_AUTO_DETECTION")
                return@flatMapLatest flowOf(DetectNearestStationUseCase.Output.AccessDenied)
            }

            // Approximate degrees for bounding box query
            val deltaLat = input.maxRadiusMeters / METERS_PER_DEGREE_LATITUDE
            val cosLat = cos(Math.toRadians(input.latitude)).coerceAtLeast(0.01)
            val deltaLng = input.maxRadiusMeters / (METERS_PER_DEGREE_LATITUDE * cosLat)

            val minLat = input.latitude - deltaLat
            val maxLat = input.latitude + deltaLat
            val minLng = input.longitude - deltaLng
            val maxLng = input.longitude + deltaLng

            stationRepository.getStationsInBoundingBox(minLat, maxLat, minLng, maxLng)
                .map { stations ->
                    val nearbyStations = stations.mapNotNull { station ->
                        val distance = haversineDistanceMeters(
                            lat1 = input.latitude,
                            lon1 = input.longitude,
                            lat2 = station.latitude,
                            lon2 = station.longitude
                        )
                        if (distance <= input.maxRadiusMeters) {
                            station to distance
                        } else {
                            null
                        }
                    }.sortedBy { it.second }

                    val nearest = nearbyStations.firstOrNull()
                    if (nearest != null) {
                        logger.i(
                            "DetectNearestStationUseCase",
                            "Nearest station found: ${nearest.first.name} (${nearest.second.toInt()}m away)"
                        )
                        DetectNearestStationUseCase.Output.Found(
                            station = nearest.first,
                            distanceMeters = nearest.second
                        )
                    } else {
                        logger.d("DetectNearestStationUseCase", "No station within ${input.maxRadiusMeters}m")
                        DetectNearestStationUseCase.Output.NotFound
                    }
                }
        }
    }

    private fun haversineDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(rLat1) * cos(rLat2) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    companion object {
        private const val EARTH_RADIUS_METERS = 6_371_000.0
        private const val METERS_PER_DEGREE_LATITUDE = 111_320.0
    }
}
