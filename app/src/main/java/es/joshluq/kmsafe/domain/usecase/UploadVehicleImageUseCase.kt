package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Use case to handle vehicle image uploads to remote storage.
 */
class UploadVehicleImageUseCase @Inject constructor(
    private val repository: RentingRepository,
    private val logger: LoggerKit
) : FlowUseCase<UploadVehicleImageUseCase.Input, UploadVehicleImageUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("UploadVehicleImageUseCase", "Starting upload for file: ${input.fileName}")
        return repository.uploadVehicleImage(input.imageBytes, input.fileName)
            .map { imageUrl ->
                logger.i("UploadVehicleImageUseCase", "Upload successful: $imageUrl")
                Output.Success(imageUrl) as Output
            }
            .onStart { emit(Output.Progress) }
            .catch { e ->
                logger.e("UploadVehicleImageUseCase", "Upload failed", e)
                emit(Output.Failure)
            }
    }

    data class Input(val imageBytes: ByteArray, val fileName: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        data class Success(val imageUrl: String) : Output
    }
}
