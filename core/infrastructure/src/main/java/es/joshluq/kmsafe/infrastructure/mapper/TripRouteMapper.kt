package es.joshluq.kmsafe.infrastructure.mapper

import es.joshluq.kmsafe.infrastructure.remote.response.TripRouteResponse
import es.joshluq.kmsafe.domain.model.TripRoute

/**
 * Maps [TripRouteResponse] to domain [TripRoute].
 */
fun TripRouteResponse.toDomain(): TripRoute = TripRoute(
    recordId = recordId ?: "",
    encodedPolyline = encodedPolyline ?: "",
    pointCount = pointCount ?: 0
)
