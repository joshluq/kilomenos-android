package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.BluetoothRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Domain UseCase to observe real-time Bluetooth connection status for an active vehicle contract.
 */
interface ObserveVehicleBluetoothConnectionUseCase :
    FlowUseCase<ObserveVehicleBluetoothConnectionUseCase.Input, ObserveVehicleBluetoothConnectionUseCase.Output> {

    data class Input(val macAddress: String?) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val isConnected: Boolean) : Output
    }
}

class ObserveVehicleBluetoothConnectionUseCaseImpl @Inject constructor(
    private val bluetoothRepository: BluetoothRepository,
    private val logger: LoggerKit
) : ObserveVehicleBluetoothConnectionUseCase {

    override fun invoke(input: ObserveVehicleBluetoothConnectionUseCase.Input): Flow<ObserveVehicleBluetoothConnectionUseCase.Output> {
        val mac = input.macAddress
        if (mac.isNullOrBlank()) {
            logger.d("ObserveVehicleBluetoothConnectionUseCase", "No MAC address provided. Emitting disconnected.")
            return flowOf(ObserveVehicleBluetoothConnectionUseCase.Output.Success(isConnected = false))
        }

        logger.d("ObserveVehicleBluetoothConnectionUseCase", "Observing Bluetooth connection for $mac")
        return bluetoothRepository.observeDeviceConnection(mac)
            .distinctUntilChanged()
            .map { isConnected ->
                ObserveVehicleBluetoothConnectionUseCase.Output.Success(isConnected = isConnected)
            }
    }
}
