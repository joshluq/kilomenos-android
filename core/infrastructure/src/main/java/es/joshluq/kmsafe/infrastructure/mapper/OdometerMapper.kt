package es.joshluq.kmsafe.infrastructure.mapper

import es.joshluq.kmsafe.infrastructure.remote.response.OdometerRecordResponse
import es.joshluq.kmsafe.domain.model.OdometerRecord
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Maps [OdometerRecordResponse] to domain [OdometerRecord].
 */
fun OdometerRecordResponse.toDomain(): OdometerRecord {
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

    return OdometerRecord(
        id = id ?: "",
        contractId = contractId ?: "",
        timestamp = parsedDate,
        odometerValue = odometerValue ?: 0,
        isInitialRecord = isInitialRecord ?: false,
        label = label,
        fuelAmount = fuelConsumed,
        hasRoute = hasRoute ?: false
    )
}

/**
 * Formats a [Long] timestamp to ISO 8601 string.
 */
fun Long.toIsoString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return sdf.format(Date(this))
}
