package es.joshluq.kmsafe.data.remote.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request body for updating an existing renting contract.
 */
data class UpdateRentingContractRequest(
    @JsonProperty("vehicle_name") val vehicleName: String,
    @JsonProperty("start_date") val startDate: String,
    @JsonProperty("duration_months") val durationMonths: Int,
    @JsonProperty("total_kms") val totalKms: Int,
    @JsonProperty("start_odometer") val startOdometer: Int,
    @JsonProperty("vehicle_image_url") val vehicleImageUrl: String? = null,
    @JsonProperty("bluetooth_device_address") val bluetoothDeviceAddress: String? = null
)
