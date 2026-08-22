package es.joshluq.kmsafe.domain.model

/**
 * Domain model containing price volatility statistics and insights for a specific service station.
 *
 * @property stationId ID of the evaluated service station.
 * @property fuelType Energy or fuel type evaluated.
 * @property currentPrice Most recently recorded unit price.
 * @property historicalAveragePrice Average unit price from historical entries at this station.
 * @property minRecordedPrice Lowest price ever recorded at this station.
 * @property maxRecordedPrice Highest price ever recorded at this station.
 * @property priceTrend Comparative price status relative to the user's historical average.
 * @property priceHistory Chronological points representing previous price updates.
 */
data class StationPriceVolatility(
    val stationId: String,
    val fuelType: FuelType,
    val currentPrice: Double,
    val historicalAveragePrice: Double,
    val minRecordedPrice: Double,
    val maxRecordedPrice: Double,
    val priceTrend: PriceTrend,
    val priceHistory: List<PricePoint> = emptyList()
)

/**
 * Single historical price record point for charts and volatility trends.
 */
data class PricePoint(
    val timestamp: Long,
    val unitPrice: Double
)

/**
 * Trend category comparing current price to historical benchmarks.
 */
enum class PriceTrend {
    CHEAPER,
    AVERAGE,
    EXPENSIVE
}
