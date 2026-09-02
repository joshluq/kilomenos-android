package es.joshluq.kmsafe.domain.model

import kotlinx.serialization.Serializable

/**
 * Immutable Value Object representing the consolidated metrics of a renting contract,
 * calculated by the KiloMenos algorithm.
 */
@Serializable
data class ContractMetrics(
    val contract: RentingContract,
    val actualKmsDriven: Double,
    val currentOdometer: Double,
    val theoreticalKms: Double,
    val balance: Double,
    val dailyBudget: Double,
    val monthlyBudget: Double,
    val timePercentage: Float,
    val kmsPercentage: Float,
    val differencePercentage: Float,
    val isSyncPending: Boolean
)
