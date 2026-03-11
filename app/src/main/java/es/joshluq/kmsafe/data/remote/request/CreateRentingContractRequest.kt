package es.joshluq.kmsafe.data.remote.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request body for creating a new renting contract.
 */
data class CreateRentingContractRequest(
    @JsonProperty("vehicle_name") val vehicleName: String,
    @JsonProperty("start_date") val startDate: String,
    @JsonProperty("duration_months") val durationMonths: Int,
    @JsonProperty("total_kms") val totalKms: Int,
    @JsonProperty("start_odometer") val startOdometer: Int,
    @JsonProperty("current_odometer") val currentOdometer: Int,
    @JsonProperty("is_selected") val isSelected: Boolean,
    @JsonProperty("vehicle_image_url") val vehicleImageUrl: String? = null
)
