package es.joshluq.kmsafe.infrastructure.remote.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response for the list of renting contracts.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class RentingListResponse : NetworkResponse() {
    @JsonProperty("contracts")
    val contracts: List<RentingContractResponse>? = null
}

/**
 * Remote model for a Renting Contract.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class RentingContractResponse {
    @JsonProperty("id")
    val id: String? = null

    @JsonProperty("user_id")
    val userId: String? = null

    @JsonProperty("vehicle_name")
    val vehicleName: String? = null

    @JsonProperty("start_date")
    val startDate: String? = null

    @JsonProperty("duration_months")
    val durationMonths: Int? = null

    @JsonProperty("total_kms")
    val totalKms: Double? = null

    @JsonProperty("start_odometer")
    val startOdometer: Double? = null

    @JsonProperty("current_odometer")
    val currentOdometer: Double? = null

    @JsonProperty("is_selected")
    val isSelected: Boolean? = null

    @JsonProperty("vehicle_image_url")
    val vehicleImageUrl: String? = null

    @JsonProperty("bluetooth_device_address")
    val bluetoothDeviceAddress: String? = null

    @JsonProperty("bluetooth_device_name")
    val bluetoothDeviceName: String? = null

    @JsonProperty("excess_km_price")
    val excessKmPrice: Double? = null

    @JsonProperty("courtesy_km_buffer")
    val courtesyKmBuffer: Int? = null

    @JsonProperty("fuel_type")
    val fuelType: String? = null
}
