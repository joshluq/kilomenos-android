package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.MediaRepository
import javax.inject.Inject

/**
 * Use case to read bytes from a media reference.
 */
class GetImageBytesUseCase @Inject constructor(
    private val repository: MediaRepository
) : UseCase<GetImageBytesUseCase.Input, GetImageBytesUseCase.Output> {

    override suspend fun invoke(input: Input): Result<Output> {
        val bytes = repository.getFileBytes(input.uriPath)
        return if (bytes != null) {
            Result.success(Output.Success(bytes))
        } else {
            Result.failure(Exception("Could not read bytes from path: ${input.uriPath}"))
        }
    }

    data class Input(val uriPath: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val bytes: ByteArray) : Output {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as Success
                return bytes.contentEquals(other.bytes)
            }
            override fun hashCode(): Int = bytes.contentHashCode()
        }
    }
}
