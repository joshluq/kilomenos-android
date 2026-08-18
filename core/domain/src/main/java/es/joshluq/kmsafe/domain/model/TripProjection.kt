package es.joshluq.kmsafe.domain.model

/**
 * Domain model representing the predictive projection of the contract's end state.
 *
 * @property projectedTotalKms Estimated total odometer reading at the end of the contract.
 * @property expectedFinalBalance Predicted kilometer balance (positive is safe, negative is excess).
 * @property isOverLimit True if the projection indicates exceeding the contracted mileage.
 * @property dailyAverage Actual real kilometer average per day based on history.
 * @property hasEnoughData True if there is enough historical data to provide a reliable projection.
 */
data class TripProjection(
    val projectedTotalKms: Int,
    val expectedFinalBalance: Int,
    val isOverLimit: Boolean,
    val dailyAverage: Double,
    val hasEnoughData: Boolean
)
