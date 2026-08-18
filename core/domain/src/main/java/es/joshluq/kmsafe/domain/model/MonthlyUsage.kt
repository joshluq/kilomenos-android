package es.joshluq.kmsafe.domain.model

/**
 * Domain model representing the monthly usage calculation for charts.
 */
data class MonthlyUsage(
    val month: String,
    val kms: Int,
    val isOverLimit: Boolean
)
