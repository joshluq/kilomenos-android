package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.BluetoothRepository
import javax.inject.Inject

/**
 * Domain UseCase to notify the system of an active vehicle Bluetooth connection/disconnection event.
 */
interface UpdateBluetoothConnectionStateUseCase :
    UseCase<UpdateBluetoothConnectionStateUseCase.Input, UpdateBluetoothConnectionStateUseCase.Output> {

    data class Input(
        val macAddress: String,
        val isConnected: Boolean
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Success : Output
    }
}

class UpdateBluetoothConnectionStateUseCaseImpl @Inject constructor(
    private val bluetoothRepository: BluetoothRepository,
    private val logger: LoggerKit
) : UpdateBluetoothConnectionStateUseCase {

    override suspend fun invoke(input: UpdateBluetoothConnectionStateUseCase.Input): Result<UpdateBluetoothConnectionStateUseCase.Output> {
        logger.d("UpdateBluetoothConnectionStateUseCase", "Updating state for ${input.macAddress}: isConnected=${input.isConnected}")
        bluetoothRepository.updateDeviceConnectionState(input.macAddress, input.isConnected)
        return Result.success(UpdateBluetoothConnectionStateUseCase.Output.Success)
    }
}
