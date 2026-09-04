package es.joshluq.kmsafe.domain.model

/**
 * Domain model representing a station in the "My Stations" price radar.
 *
 * @property station The service station entity.
 * @property fuelType Evaluated fuel or energy type.
 * @property lastRecordedPrice Most recent unit price recorded at this station.
 * @property userAveragePrice User's historical average unit price at this station.
 * @property priceDelta Difference between last recorded price and user average price.
 * @property isOpportunity True if last price is significantly lower than average.
 * @property bestDayPrediction Optional suggestion for the optimal day of the week to refuel.
 */
data class StationRadarItem(
    val station: ServiceStation,
    val fuelType: FuelType,
    val lastRecordedPrice: Double,
    val userAveragePrice: Double,
    val priceDelta: Double,
    val isOpportunity: Boolean,
    val bestDayPrediction: String? = null
)
