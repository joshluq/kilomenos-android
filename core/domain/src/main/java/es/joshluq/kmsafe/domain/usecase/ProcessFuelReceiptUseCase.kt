package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.domain.model.ReceiptScanResult
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.repository.AuthRepository
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import es.joshluq.kmsafe.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import java.util.UUID
import javax.inject.Inject

/**
 * Domain interface to process a fuel receipt using AI OCR.
 */
interface ProcessFuelReceiptUseCase : FlowUseCase<ProcessFuelReceiptUseCase.Input, ProcessFuelReceiptUseCase.Output> {

    data class Input(
        val uriPath: String,
        val vehicleId: String
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Success(val result: ReceiptScanResult) : Output
        data class Failure(val error: KmError) : Output
    }
}

class ProcessFuelReceiptUseCaseImpl @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val authRepository: AuthRepository,
    private val entitlementsRepository: EntitlementsRepository,
    private val logger: LoggerKit
) : ProcessFuelReceiptUseCase {

    override fun invoke(input: ProcessFuelReceiptUseCase.Input): Flow<ProcessFuelReceiptUseCase.Output> = flow {
        logger.d("ProcessFuelReceiptUseCase", "Processing receipt for vehicle: ${input.vehicleId}")

        val user = authRepository.getCurrentUser().first()
        if (user == null) {
            logger.w("ProcessFuelReceiptUseCase", "Unauthenticated user attempting receipt scan")
            emit(ProcessFuelReceiptUseCase.Output.Failure(KmError.Unauthenticated))
            return@flow
        }

        val entitlements = entitlementsRepository.observeEntitlements().first()
        val hasAccess = entitlements.subscriptionLevel == SubscriptionLevel.PREMIUM || entitlements.isTrialActive

        if (!hasAccess) {
            logger.w("ProcessFuelReceiptUseCase", "Non-premium user attempting receipt scan without active trial")
            emit(ProcessFuelReceiptUseCase.Output.Failure(KmError.FuelExpensesPremiumOnly))
            return@flow
        }

        val timestamp = System.currentTimeMillis()
        val suffix = UUID.randomUUID().toString().take(8)
        val fileName = "receipt-$timestamp-$suffix.jpg"

        logger.d("ProcessFuelReceiptUseCase", "Uploading image from URI: ${input.uriPath}")
        val filePath = receiptRepository.uploadReceiptFromUri(
            userId = user.id,
            fileName = fileName,
            uriPath = input.uriPath
        ).first()

        logger.d("ProcessFuelReceiptUseCase", "Extracting OCR data from filePath: $filePath")
        val scanResult = receiptRepository.processReceipt(filePath).first()

        if (!scanResult.isFuelReceipt) {
            logger.w("ProcessFuelReceiptUseCase", "Uploaded document is not a valid fuel receipt: ${scanResult.stationName}")
            try {
                receiptRepository.deleteReceiptImage(filePath).first()
            } catch (e: Exception) {
                logger.w("ProcessFuelReceiptUseCase", "Failed to purge non-fuel receipt image", e)
            }
            emit(ProcessFuelReceiptUseCase.Output.Failure(KmError.InvalidReceiptImage))
            return@flow
        }

        logger.i("ProcessFuelReceiptUseCase", "Receipt extracted successfully for ${scanResult.stationName}, total=${scanResult.totalAmount}")
        emit(ProcessFuelReceiptUseCase.Output.Success(scanResult))
    }
        .onStart { emit(ProcessFuelReceiptUseCase.Output.Progress) }
        .catch { e ->
            logger.e("ProcessFuelReceiptUseCase", "Failed to process receipt", e)
            val error = (e as? KmException)?.error ?: KmError.NetworkError
            emit(ProcessFuelReceiptUseCase.Output.Failure(error))
        }
}
