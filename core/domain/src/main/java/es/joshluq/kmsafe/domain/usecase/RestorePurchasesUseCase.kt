package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.BillingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Domain use case to restore and reconcile active Google Play subscriptions.
 */
interface RestorePurchasesUseCase : FlowUseCase<RestorePurchasesUseCase.Input, RestorePurchasesUseCase.Output> {

    data object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Success(val restoredCount: Int) : Output
        data object NoPurchasesFound : Output
        data class Failure(val message: String) : Output
    }
}

class RestorePurchasesUseCaseImpl @Inject constructor(
    private val billingRepository: BillingRepository,
    private val logger: LoggerKit
) : RestorePurchasesUseCase {

    override fun invoke(input: RestorePurchasesUseCase.Input): Flow<RestorePurchasesUseCase.Output> {
        logger.d("RestorePurchasesUseCase", "Initiating purchase restoration")
        return billingRepository.restorePurchases()
            .map { restoredCount ->
                logger.i("RestorePurchasesUseCase", "Restored count: $restoredCount")
                if (restoredCount > 0) {
                    RestorePurchasesUseCase.Output.Success(restoredCount) as RestorePurchasesUseCase.Output
                } else {
                    RestorePurchasesUseCase.Output.NoPurchasesFound
                }
            }
            .onStart { emit(RestorePurchasesUseCase.Output.Progress) }
            .catch { error ->
                logger.e("RestorePurchasesUseCase", "Error during purchase restoration", error)
                emit(RestorePurchasesUseCase.Output.Failure(error.message ?: "Error al restaurar las compras"))
            }
    }
}
