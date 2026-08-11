package es.joshluq.kmsafe.data.remote.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request body for uploading a trip route.
 */
data class UploadRouteRequest(
    @JsonProperty("encoded_polyline") val encodedPolyline: String,
    @JsonProperty("point_count") val pointCount: Int
)
