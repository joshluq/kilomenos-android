package es.joshluq.kmsafe.infrastructure.remote.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import es.joshluq.kmsafe.domain.model.ArithmeticCheck
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.ReceiptScanResult

/**
 * Remote fuel types supported by the OCR Edge Function.
 */
enum class RemoteFuelType {
    @JsonProperty("GASOLINE_95") GASOLINE_95,
    @JsonProperty("GASOLINE_98") GASOLINE_98,
    @JsonProperty("DIESEL") DIESEL,
    @JsonProperty("DIESEL_PLUS") DIESEL_PLUS,
    @JsonProperty("LPG") LPG,
    @JsonProperty("CNG") CNG,
    @JsonProperty("ELECTRIC_KWH") ELECTRIC_KWH,
    @JsonProperty("HYBRID_PHEV") HYBRID_PHEV,
    @JsonProperty("HYDROGEN") HYDROGEN,
    @JsonProperty("BIODIESEL") BIODIESEL,
    @JsonProperty("ETHANOL_E85") ETHANOL_E85,
    @JsonProperty("ADBLUE") ADBLUE,
    @JsonProperty("OTHER") OTHER;

    fun toDomain(): FuelType = when (this) {
        GASOLINE_95 -> FuelType.GASOLINE_95
        GASOLINE_98 -> FuelType.GASOLINE_98
        DIESEL -> FuelType.DIESEL
        DIESEL_PLUS -> FuelType.DIESEL_PLUS
        LPG -> FuelType.LPG
        CNG -> FuelType.CNG
        ELECTRIC_KWH -> FuelType.ELECTRIC_KWH
        HYBRID_PHEV -> FuelType.GASOLINE_95 // default primary combustion for hybrid
        HYDROGEN -> FuelType.HYDROGEN
        BIODIESEL -> FuelType.BIODIESEL
        ETHANOL_E85 -> FuelType.ETHANOL_E85
        ADBLUE -> FuelType.ADBLUE
        OTHER -> FuelType.OTHER
    }
}

/**
 * Request payload sent to /process-receipt Edge Function.
 */
data class ProcessReceiptRequest(
    @JsonProperty("filePath") val filePath: String
)

/**
 * Coherence check sub-object returned by /process-receipt.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ArithmeticCheckDto(
    @JsonProperty("valid") val valid: Boolean = false,
    @JsonProperty("calculated_amount") val calculatedAmount: Double = 0.0,
    @JsonProperty("discrepancy") val discrepancy: Double = 0.0
) {
    fun toDomain(): ArithmeticCheck = ArithmeticCheck(
        valid = valid,
        calculatedAmount = calculatedAmount,
        discrepancy = discrepancy
    )
}

/**
 * Extracted receipt data returned by /process-receipt.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ExtractedReceiptDataDto(
    @JsonProperty("station_name") val stationName: String? = null,
    @JsonProperty("purchase_date") val purchaseDate: String? = null,
    @JsonProperty("fuel_type") val fuelType: RemoteFuelType? = null,
    @JsonProperty("liters") val liters: Double? = null,
    @JsonProperty("price_per_liter") val pricePerLiter: Double? = null,
    @JsonProperty("total_amount") val totalAmount: Double? = null,
    @JsonProperty("currency") val currency: String? = "EUR",
    @JsonProperty("confidence_score") val confidenceScore: Double? = null,
    @JsonProperty("is_fuel_receipt") val isFuelReceipt: Boolean? = null,
    @JsonProperty("arithmetic_check") val arithmeticCheck: ArithmeticCheckDto? = null
) {
    fun toDomain(storageFilePath: String): ReceiptScanResult = ReceiptScanResult(
        stationName = stationName.orEmpty(),
        purchaseDate = purchaseDate.orEmpty(),
        fuelType = fuelType?.toDomain() ?: FuelType.GASOLINE_95,
        liters = liters ?: 0.0,
        pricePerLiter = pricePerLiter ?: 0.0,
        totalAmount = totalAmount ?: 0.0,
        currency = currency ?: "EUR",
        confidenceScore = confidenceScore ?: 0.0,
        isFuelReceipt = isFuelReceipt ?: false,
        arithmeticCheck = arithmeticCheck?.toDomain() ?: ArithmeticCheck(false, 0.0, 0.0),
        storageFilePath = storageFilePath
    )
}

/**
 * Response payload returned by /process-receipt.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ProcessReceiptResponse(
    @JsonProperty("success") val success: Boolean = false,
    @JsonProperty("data") val data: ExtractedReceiptDataDto? = null,
    @JsonProperty("error") val error: String? = null
)
