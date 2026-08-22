package es.joshluq.kmsafe.infrastructure.mapper

import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.ServiceStation
import es.joshluq.kmsafe.infrastructure.remote.request.StationRequest
import es.joshluq.kmsafe.infrastructure.remote.response.StationRemoteModel

/**
 * Maps domain [ServiceStation] to [StationRequest] for synchronization.
 */
fun ServiceStation.toRequest(): StationRequest {
    return StationRequest(
        id = id,
        name = name,
        brand = brand,
        latitude = latitude,
        longitude = longitude,
        address = address,
        isFavorite = isFavorite,
        availableEnergies = availableEnergies.map { it.name },
        createdAt = createdAt.toIsoString(),
        updatedAt = updatedAt.toIsoString()
    )
}

/**
 * Maps [StationRemoteModel] to domain [ServiceStation].
 */
fun StationRemoteModel.toDomain(): ServiceStation {
    // Helper to parse backend ISO 8601 strings
    fun String?.toMillis(): Long {
        if (this == null) return System.currentTimeMillis()
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        return formats.firstNotNullOfOrNull { format ->
            val sdf = java.text.SimpleDateFormat(format, java.util.Locale.getDefault())
            if (!format.contains("X") && !format.contains("'Z'")) {
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            val pos = java.text.ParsePosition(0)
            val date = sdf.parse(this, pos)
            if (pos.index > 0) date else null
        }?.time ?: System.currentTimeMillis()
    }

    return ServiceStation(
        id = id ?: "",
        name = name ?: "",
        brand = brand ?: "",
        latitude = latitude ?: 0.0,
        longitude = longitude ?: 0.0,
        address = address ?: "",
        isFavorite = isFavorite ?: false,
        availableEnergies = availableEnergies?.map { FuelType.fromName(it) } ?: emptyList(),
        createdAt = createdAt.toMillis(),
        updatedAt = updatedAt.toMillis()
    )
}
