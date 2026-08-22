package es.joshluq.kmsafe.core.domain.service

import es.joshluq.kmsafe.domain.model.ServiceStation
import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing virtual perimeters (geofences) around service stations.
 */
interface GeofenceService {

    /**
     * Registers geofences for the provided list of stations.
     * Existing geofences managed by this service will be replaced.
     */
    fun registerStationGeofences(stations: List<ServiceStation>): Flow<Unit>

    /**
     * Removes all geofences registered for service stations.
     */
    fun clearAllGeofences(): Flow<Unit>
}
