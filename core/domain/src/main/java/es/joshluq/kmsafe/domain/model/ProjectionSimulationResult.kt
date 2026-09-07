package es.joshluq.kmsafe.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain value object encapsulating the full mathematical outcome of a contract pacing simulation.
 *
 * @property simulatedProjectedTotalKms Total expected odometer value upon contract expiration.
 * @property simulatedFinalBalance Projected kilometer balance (positive = surplus, negative = deficit).
 * @property estimatedPenalty Financial excess penalty in Euros based on the excess rate.
 * @property exhaustionDateMillis Timestamp in epoch milliseconds when the contract km allowance runs out, or null if within budget.
 * @property monthsAheadOrBehind Number of months difference between exhaustion date and contract end date (positive if exhausts early).
 * @property remedialDailyKm Target daily rate (km/day) starting today to reach 0 € penalty at contract end, or null if expired.
 * @property isOverLimit True if the simulation results in a mileage penalty.
 */
@Serializable
data class ProjectionSimulationResult(
    val simulatedProjectedTotalKms: Double,
    val simulatedFinalBalance: Double,
    val estimatedPenalty: Double,
    val exhaustionDateMillis: Long?,
    val monthsAheadOrBehind: Int,
    val remedialDailyKm: Double?,
    val isOverLimit: Boolean
)
