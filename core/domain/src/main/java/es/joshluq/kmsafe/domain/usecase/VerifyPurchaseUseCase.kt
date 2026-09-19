package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.VerifyPurchaseResult
import es.joshluq.kmsafe.domain.repository.BillingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Domain use case to verify an in-app Google Play subscription purchase with the backend.
 */
interface VerifyPurchaseUseCase : FlowUseCase<VerifyPurchaseUseCase.Input, VerifyPurchaseUseCase.Output> {

    data class Input(
        val purchaseToken: String,
        val subscriptionId: String,
        val packageName: String? = null
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Success(val result: VerifyPurchaseResult) : Output
        data class Failure(val message: String) : Output
    }
}

class VerifyPurchaseUseCaseImpl @Inject constructor(
    private val billingRepository: BillingRepository,
    private val logger: LoggerKit
) : VerifyPurchaseUseCase {

    override fun invoke(input: VerifyPurchaseUseCase.Input): Flow<VerifyPurchaseUseCase.Output> {
        logger.d("VerifyPurchaseUseCase", "Verifying purchase with subscriptionId: ${input.subscriptionId}")
        return billingRepository.verifyPurchase(
            purchaseToken = input.purchaseToken,
            subscriptionId = input.subscriptionId,
            packageName = input.packageName
        )
            .map { result ->
                if (result.success) {
                    logger.i("VerifyPurchaseUseCase", "Purchase verified successfully: ${result.message}")
                    VerifyPurchaseUseCase.Output.Success(result) as VerifyPurchaseUseCase.Output
                } else {
                    val errorMsg = result.message ?: "Verification failed"
                    logger.e("VerifyPurchaseUseCase", "Purchase verification rejected: $errorMsg")
                    VerifyPurchaseUseCase.Output.Failure(errorMsg)
                }
            }
            .onStart { emit(VerifyPurchaseUseCase.Output.Progress) }
            .catch { error ->
                logger.e("VerifyPurchaseUseCase", "Error during purchase verification", error)
                emit(VerifyPurchaseUseCase.Output.Failure(error.message ?: "Error al verificar la compra"))
            }
    }
}
