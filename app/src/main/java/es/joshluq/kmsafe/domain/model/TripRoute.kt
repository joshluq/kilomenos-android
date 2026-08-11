package es.joshluq.kmsafe.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain model representing a captured trip route.
 */
@Serializable
data class TripRoute(
    val recordId: String,
    val encodedPolyline: String,
    val pointCount: Int
)
