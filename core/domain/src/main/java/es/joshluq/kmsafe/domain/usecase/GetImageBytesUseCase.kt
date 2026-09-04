package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.MediaRepository
import javax.inject.Inject

/**
 * Domain interface to read bytes from a media reference.
 */
interface GetImageBytesUseCase : UseCase<GetImageBytesUseCase.Input, GetImageBytesUseCase.Output> {

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

class GetImageBytesUseCaseImpl @Inject constructor(
    private val repository: MediaRepository
) : GetImageBytesUseCase {

    override suspend fun invoke(input: GetImageBytesUseCase.Input): Result<GetImageBytesUseCase.Output> {
        val bytes = repository.getFileBytes(input.uriPath)
        return if (bytes != null) {
            Result.success(GetImageBytesUseCase.Output.Success(bytes))
        } else {
            Result.failure(Exception("Could not read bytes from path: ${input.uriPath}"))
        }
    }
}
