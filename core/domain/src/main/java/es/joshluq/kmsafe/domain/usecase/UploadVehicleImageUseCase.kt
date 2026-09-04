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
 * Domain interface to handle vehicle image uploads to remote storage.
 */
interface UploadVehicleImageUseCase : FlowUseCase<UploadVehicleImageUseCase.Input, UploadVehicleImageUseCase.Output> {

    data class Input(val imageBytes: ByteArray, val fileName: String) : UseCaseInput {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Input

            if (!imageBytes.contentEquals(other.imageBytes)) return false
            if (fileName != other.fileName) return false

            return true
        }

        override fun hashCode(): Int {
            var result = imageBytes.contentHashCode()
            result = 31 * result + fileName.hashCode()
            return result
        }
    }

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        data class Success(val imageUrl: String) : Output
    }
}

class UploadVehicleImageUseCaseImpl @Inject constructor(
    private val repository: RentingRepository,
    private val logger: LoggerKit
) : UploadVehicleImageUseCase {

    override fun invoke(input: UploadVehicleImageUseCase.Input): Flow<UploadVehicleImageUseCase.Output> {
        logger.d("UploadVehicleImageUseCase", "Starting upload for file: ${input.fileName}")
        return repository.uploadVehicleImage(input.imageBytes, input.fileName)
            .map { imageUrl ->
                logger.i("UploadVehicleImageUseCase", "Upload successful: $imageUrl")
                UploadVehicleImageUseCase.Output.Success(imageUrl) as UploadVehicleImageUseCase.Output
            }
            .onStart { emit(UploadVehicleImageUseCase.Output.Progress) }
            .catch { e ->
                logger.e("UploadVehicleImageUseCase", "Upload failed", e)
                emit(UploadVehicleImageUseCase.Output.Failure)
            }
    }
}
