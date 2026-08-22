package es.joshluq.kmsafe.domain.model

/**
 * Domain Value Object representing the raw, unformatted odometer usage aggregation for a specific month.
 * Pure Kotlin, completely decoupled from Android framework or UI representations.
 */
data class MonthlyOdometerAggregation(
    val year: Int,
    val month: Int,
    val totalKms: Double,
    val budgetedKms: Double
)
