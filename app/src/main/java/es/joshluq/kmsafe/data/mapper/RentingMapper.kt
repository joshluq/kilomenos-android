package es.joshluq.kmsafe.data.mapper

import es.joshluq.kmsafe.data.remote.response.RentingContractResponse
import es.joshluq.kmsafe.domain.model.RentingContract
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Maps [RentingContractResponse] to domain [RentingContract].
 */
fun RentingContractResponse.toDomain(): RentingContract {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    val parsedDate = runCatching {
        // Handle potential offset or extra precision in ISO string (e.g., 2026-07-21T12:00:00+00:00)
        // We take the first 19 chars for standard parsing if it's long
        val dateStr = startDate?.substring(0, 19) ?: ""
        sdf.parse(dateStr)?.time ?: 0L
    }.getOrDefault(0L)

    return RentingContract(
        id = id ?: "",
        userId = userId ?: "",
        vehicleName = vehicleName ?: "Unknown",
        startDate = parsedDate,
        durationMonths = durationMonths ?: 0,
        totalKms = totalKms ?: 0,
        startOdometer = startOdometer ?: 0,
        currentOdometer = currentOdometer ?: 0,
        isSelected = isSelected ?: false,
        vehicleImageUrl = vehicleImageUrl,
        bluetoothDeviceAddress = bluetoothDeviceAddress
    )
}
