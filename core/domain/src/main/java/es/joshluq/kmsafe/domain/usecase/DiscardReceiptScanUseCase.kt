package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Domain interface to discard a scanned receipt and purge its image from cloud storage.
 */
interface DiscardReceiptScanUseCase : FlowUseCase<DiscardReceiptScanUseCase.Input, DiscardReceiptScanUseCase.Output> {

    data class Input(val filePath: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Success : Output
    }
}

class DiscardReceiptScanUseCaseImpl @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val logger: LoggerKit
) : DiscardReceiptScanUseCase {

    override fun invoke(input: DiscardReceiptScanUseCase.Input): Flow<DiscardReceiptScanUseCase.Output> = flow {
        logger.d("DiscardReceiptScanUseCase", "Purging discarded receipt file: ${input.filePath}")
        if (input.filePath.isNotBlank()) {
            receiptRepository.deleteReceiptImage(input.filePath).first()
        }
        emit(DiscardReceiptScanUseCase.Output.Success)
    }.catch { e ->
        logger.w("DiscardReceiptScanUseCase", "Non-fatal error discarding receipt: ${input.filePath}", e)
        emit(DiscardReceiptScanUseCase.Output.Success)
    }
}
