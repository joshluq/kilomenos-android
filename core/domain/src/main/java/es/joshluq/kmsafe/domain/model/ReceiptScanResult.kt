package es.joshluq.kmsafe.domain.model

/**
 * Domain model representing the structured extraction of a fuel receipt processed by AI.
 *
 * @property stationName Name of the service station detected on the receipt.
 * @property purchaseDate ISO-8601 string of the purchase date.
 * @property fuelType Energy or fuel type consumed.
 * @property liters Volume of fuel in liters (or kWh for electric).
 * @property pricePerLiter Unit price per liter or per kWh.
 * @property totalAmount Total monetary amount (€) paid.
 * @property currency Currency code (defaults to "EUR").
 * @property confidenceScore Overall extraction confidence score between 0.0 and 1.0.
 * @property isFuelReceipt True if the document was classified as a valid fuel/recharge receipt.
 * @property arithmeticCheck Mathematical check verifying liters * pricePerLiter ≈ totalAmount.
 * @property storageFilePath Relative file path in Supabase Storage (e.g. "userId/receipt-123.jpg").
 */
data class ReceiptScanResult(
    val stationName: String,
    val purchaseDate: String,
    val fuelType: FuelType,
    val liters: Double,
    val pricePerLiter: Double,
    val totalAmount: Double,
    val currency: String = "EUR",
    val confidenceScore: Double,
    val isFuelReceipt: Boolean,
    val arithmeticCheck: ArithmeticCheck,
    val storageFilePath: String
)
