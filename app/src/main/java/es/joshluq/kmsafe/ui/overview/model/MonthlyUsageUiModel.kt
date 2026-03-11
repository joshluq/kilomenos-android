package es.joshluq.kmsafe.ui.overview.model

/**
 * UI representation model for monthly odometer usage.
 * Ready to be consumed directly by Jetpack Compose without business logic processing.
 */
data class MonthlyUsageUiModel(
    val monthName: String,
    val kmsText: String,
    val budgetedKmsText: String,
    val barPercentage: Float,
    val budgetPercentage: Float,
    val limitState: LimitState
) {
    /**
     * Represents the visual state of the usage bar based on business rules.
     */
    enum class LimitState {
        SAFE, // Below budget (VividGreen)
        OVER_LIMIT // Exceeded budget (Red)
    }
}
