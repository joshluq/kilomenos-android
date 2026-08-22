package es.joshluq.kmsafe.domain.model

/**
 * Domain model representing aggregated statistics and history for a specific service station.
 */
data class ServiceStationDetail(
    val station: ServiceStation,
    val totalSpent: Double,
    val totalVolume: Double,
    val refuelCount: Int,
    val expenseHistory: List<FuelExpense>,
    val averageConsumption: Double? = null
)
