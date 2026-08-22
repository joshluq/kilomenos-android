package es.joshluq.kmsafe.infrastructure.mapper

import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.infrastructure.remote.request.FuelExpenseRequest
import es.joshluq.kmsafe.infrastructure.remote.response.FuelExpenseRemoteModel
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.*

/**
 * Maps domain [FuelExpense] to [FuelExpenseRequest].
 */
fun FuelExpense.toRequest(): FuelExpenseRequest {
    return FuelExpenseRequest(
        id = id,
        timestamp = timestamp.toIsoString(),
        odometerValue = odometerAtExpense,
        volumeQuantity = volumeQuantity,
        pricePerUnit = unitPrice,
        totalCost = totalCost,
        isFullTank = isFullTank,
        fuelType = fuelType.name,
        stationId = stationId,
        stationName = stationName,
        kmSinceLastRefuel = kmSinceLastRefuel,
        consumptionPer100km = consumptionPer100km
    )
}

/**
 * Maps [FuelExpenseRemoteModel] to domain [FuelExpense].
 */
fun FuelExpenseRemoteModel.toDomain(): FuelExpense {
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss"
    )

    val parsedDate = timestamp?.let { ts ->
        formats.firstNotNullOfOrNull { format ->
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            // If the format doesn't have an offset specifier (X or Z), we assume UTC
            if (!format.contains("X") && !format.contains("'Z'")) {
                sdf.timeZone = TimeZone.getTimeZone("UTC")
            }

            val pos = ParsePosition(0)
            val date = sdf.parse(ts, pos)

            if (pos.index > 0) date else null
        }?.time
    } ?: 0L

    return FuelExpense(
        id = id ?: "",
        vehicleId = contractId ?: "",
        stationId = stationId,
        stationName = stationName,
        timestamp = parsedDate,
        fuelType = FuelType.fromName(fuelType),
        unitPrice = pricePerUnit ?: 0.0,
        volumeQuantity = volume ?: 0.0,
        totalCost = totalCost ?: 0.0,
        odometerAtExpense = odometerValue,
        isFullTank = isFullTank ?: true,
        notes = null, // Backend doesn't support notes yet according to contract
        kmSinceLastRefuel = kmSinceLastRefuel,
        consumptionPer100km = consumptionPer100km
    )
}
