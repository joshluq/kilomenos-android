package es.joshluq.kmsafe.domain.model

/**
 * Domain model representing a captured trip route.
 */
data class TripRoute(
    val recordId: String,
    val encodedPolyline: String,
    val pointCount: Int
)
