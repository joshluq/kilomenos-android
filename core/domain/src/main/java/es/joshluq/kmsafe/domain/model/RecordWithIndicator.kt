package es.joshluq.kmsafe.domain.model

/**
 * Domain model representing an Odometer Record with an indicator if it's over limit.
 */
data class RecordWithIndicator(
    val record: OdometerRecord,
    val isOverLimit: Boolean
)
