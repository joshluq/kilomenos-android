package es.joshluq.kmsafe.infrastructure.remote.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Remote request model for a Service Station.
 */
data class StationRequest(
    @JsonProperty("id") val id: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("brand") val brand: String,
    @JsonProperty("latitude") val latitude: Double,
    @JsonProperty("longitude") val longitude: Double,
    @JsonProperty("address") val address: String,
    @JsonProperty("is_favorite") val isFavorite: Boolean,
    @JsonProperty("available_energies") val availableEnergies: List<String>,
    @JsonProperty("created_at") val createdAt: String? = null,
    @JsonProperty("updated_at") val updatedAt: String? = null
)
