package es.joshluq.kmsafe.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Domain entity representing a planned upcoming trip or getaway to simulate contract impact.
 *
 * @property id Unique identifier (client-side UUID v4).
 * @property title Human-readable label (e.g., "Semana Santa", "Vacaciones de Verano").
 * @property distanceKms Additional kilometers for this specific trip.
 */
@Serializable
data class PlannedTrip(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val distanceKms: Int
)
