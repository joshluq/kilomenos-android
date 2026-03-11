package es.joshluq.kmsafe.data.mapper

import es.joshluq.kmsafe.data.remote.response.OdometerRecordResponse
import es.joshluq.kmsafe.domain.model.OdometerRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Maps [OdometerRecordResponse] to domain [OdometerRecord].
 */
fun OdometerRecordResponse.toDomain(): OdometerRecord {
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss"
    )

    val parsedDate = timestamp?.let { ts ->
        var date: Date? = null
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                date = sdf.parse(ts)
                if (date != null) break
            } catch (e: Exception) {
                // Try next format
            }
        }
        date?.time
    } ?: 0L

    return OdometerRecord(
        id = id ?: "",
        contractId = contractId ?: "",
        timestamp = parsedDate,
        odometerValue = odometerValue ?: 0,
        isInitialRecord = isInitialRecord ?: false,
        label = label,
        fuelAmount = fuelAmount
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
