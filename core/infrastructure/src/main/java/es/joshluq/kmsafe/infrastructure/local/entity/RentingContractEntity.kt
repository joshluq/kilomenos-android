package es.joshluq.kmsafe.infrastructure.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.SyncStatus

/**
 * Room Entity representing the renting contract in the local database.
 *
 * @property id Unique identifier for the contract.
 * @property userId The ID of the user who owns this contract.
 * @property vehicleName The name of the vehicle.
 * @property startDate The contract start date.
 * @property durationMonths Total months.
 * @property totalKms Total mileage.
 * @property startOdometer Initial mileage.
 * @property currentOdometer Current mileage at registration.
 * @property isSelected Whether this contract is currently selected.
 * @property vehicleImageUrl The URL of the vehicle image.
 * @property bluetoothDeviceAddress The MAC address of the paired car bluetooth.
 * @property syncStatus Current synchronization status.
 */
@Entity(tableName = "renting_contract")
data class RentingContractEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val vehicleName: String,
    val startDate: Long,
    val durationMonths: Int,
    val totalKms: Double,
    val startOdometer: Double,
    val currentOdometer: Double,
    val isSelected: Boolean = false,
    val vehicleImageUrl: String? = null,
    val bluetoothDeviceName: String? = null,
    val bluetoothDeviceAddress: String? = null,
    val excessDistancePrice: Double? = null,
    val courtesyMarginKms: Double = 0.0,
    val fuelType: String = "GASOLINE_95",
    val syncStatus: String = "SYNCED"
)

/**
 * Extension function to map Entity to Domain model.
 */
fun RentingContractEntity.toDomain(): RentingContract = RentingContract(
    id = id,
    userId = userId,
    vehicleName = vehicleName,
    startDate = startDate,
    durationMonths = durationMonths,
    totalKms = totalKms,
    startOdometer = startOdometer,
    currentOdometer = currentOdometer,
    isSelected = isSelected,
    vehicleImageUrl = vehicleImageUrl,
    bluetoothDeviceName = bluetoothDeviceName,
    bluetoothDeviceAddress = bluetoothDeviceAddress,
    excessDistancePrice = excessDistancePrice,
    courtesyMarginKms = courtesyMarginKms,
    fuelType = FuelType.fromName(fuelType),
    syncStatus = SyncStatus.valueOf(syncStatus)
)

/**
 * Extension function to map Domain model to Entity.
 */
fun RentingContract.toEntity(): RentingContractEntity = RentingContractEntity(
    id = id,
    userId = userId,
    vehicleName = vehicleName,
    startDate = startDate,
    durationMonths = durationMonths,
    totalKms = totalKms,
    startOdometer = startOdometer,
    currentOdometer = currentOdometer,
    isSelected = isSelected,
    vehicleImageUrl = vehicleImageUrl,
    bluetoothDeviceName = bluetoothDeviceName,
    bluetoothDeviceAddress = bluetoothDeviceAddress,
    excessDistancePrice = excessDistancePrice,
    courtesyMarginKms = courtesyMarginKms,
    fuelType = fuelType.name,
    syncStatus = syncStatus.name
)
