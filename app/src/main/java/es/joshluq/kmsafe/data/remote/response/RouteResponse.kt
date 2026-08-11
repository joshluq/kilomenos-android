package es.joshluq.kmsafe.data.remote.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response for a trip route.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class RouteResponse : NetworkResponse() {
    @JsonProperty("route")
    val route: TripRouteResponse? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class TripRouteResponse {
    @JsonProperty("record_id")
    val recordId: String? = null
    
    @JsonProperty("encoded_polyline")
    val encodedPolyline: String? = null
    
    @JsonProperty("point_count")
    val pointCount: Int? = null
}
