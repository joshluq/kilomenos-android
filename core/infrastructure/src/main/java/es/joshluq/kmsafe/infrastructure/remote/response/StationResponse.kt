package es.joshluq.kmsafe.infrastructure.remote.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response for the list of service stations.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class StationListResponse : NetworkResponse() {
    @JsonProperty("stations")
    val stations: List<StationRemoteModel>? = null
}

/**
 * Remote model for a Service Station.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class StationRemoteModel {
    @JsonProperty("id")
    val id: String? = null

    @JsonProperty("user_id")
    val userId: String? = null

    @JsonProperty("name")
    val name: String? = null

    @JsonProperty("brand")
    val brand: String? = null

    @JsonProperty("latitude")
    val latitude: Double? = null

    @JsonProperty("longitude")
    val longitude: Double? = null

    @JsonProperty("address")
    val address: String? = null

    @JsonProperty("is_favorite")
    val isFavorite: Boolean? = null

    @JsonProperty("available_energies")
    val availableEnergies: List<String>? = null

    @JsonProperty("created_at")
    val createdAt: String? = null

    @JsonProperty("updated_at")
    val updatedAt: String? = null
}
