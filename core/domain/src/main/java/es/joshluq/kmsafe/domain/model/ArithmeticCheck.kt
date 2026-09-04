package es.joshluq.kmsafe.domain.model

/**
 * Result of the arithmetic coherence check on an extracted receipt:
 * liters * pricePerLiter ≈ totalAmount.
 *
 * @property valid True if discrepancy is within acceptable tolerance (< 0.05).
 * @property calculatedAmount The calculated product (liters * pricePerLiter).
 * @property discrepancy The absolute difference between totalAmount and calculatedAmount.
 */
data class ArithmeticCheck(
    val valid: Boolean,
    val calculatedAmount: Double,
    val discrepancy: Double
)
