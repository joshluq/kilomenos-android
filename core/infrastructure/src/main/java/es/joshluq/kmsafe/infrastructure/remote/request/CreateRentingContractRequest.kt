package es.joshluq.kmsafe.infrastructure.remote.request

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request body for creating a new renting contract.
 */
data class CreateRentingContractRequest(
    @JsonProperty("id") val id: String,
    @JsonProperty("vehicle_name") val vehicleName: String,
    @JsonProperty("start_date") val startDate: String,
    @JsonProperty("duration_months") val durationMonths: Int,
    @JsonProperty("total_kms") val totalKms: Double,
    @JsonProperty("start_odometer") val startOdometer: Double,
    @JsonProperty("current_odometer") val currentOdometer: Double,
    @JsonProperty("is_selected") val isSelected: Boolean,
    @JsonProperty("vehicle_image_url") val vehicleImageUrl: String? = null,
    @JsonProperty("bluetooth_device_name") val bluetoothDeviceName: String? = null,
    @JsonProperty("bluetooth_device_address") val bluetoothDeviceAddress: String? = null,
    @JsonProperty("excess_km_price") val excessKmPrice: Double? = null,
    @JsonProperty("courtesy_km_buffer") val courtesyKmBuffer: Double? = null,
    @JsonProperty("fuel_type") val fuelType: String
)
