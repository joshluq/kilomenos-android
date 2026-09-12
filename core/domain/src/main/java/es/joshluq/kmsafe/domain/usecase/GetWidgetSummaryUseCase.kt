package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.WidgetSummary
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Domain UseCase that computes a consolidated glanceable snapshot of the active renting contract
 * for rendering on the Android Home Screen Widget.
 */
interface GetWidgetSummaryUseCase :
    UseCase<GetWidgetSummaryUseCase.Input, GetWidgetSummaryUseCase.Output> {

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val summary: WidgetSummary) : Output
        object NoActiveContract : Output
    }
}

class GetWidgetSummaryUseCaseImpl @Inject constructor(
    private val rentingRepository: RentingRepository,
    private val historyRepository: HistoryRepository,
    private val calculateContractMetricsUseCase: CalculateContractMetricsUseCase,
    private val logger: LoggerKit
) : GetWidgetSummaryUseCase {

    override suspend fun invoke(input: GetWidgetSummaryUseCase.Input): Result<GetWidgetSummaryUseCase.Output> {
        return runCatching {
            logger.d("GetWidgetSummaryUseCase", "Executing widget summary retrieval")
            val contract = rentingRepository.getContract().first()
            if (contract == null) {
                logger.d("GetWidgetSummaryUseCase", "No active contract found")
                return@runCatching GetWidgetSummaryUseCase.Output.NoActiveContract
            }

            val records = historyRepository.getHistory(contract.id).first()
            val metricsResult = calculateContractMetricsUseCase(
                CalculateContractMetricsUseCase.Input(contract, records)
            ).getOrThrow() as CalculateContractMetricsUseCase.Output.Success

            val metrics = metricsResult.metrics
            val isSafe = metrics.balance >= 0.0

            val summary = WidgetSummary(
                vehicleName = contract.vehicleName,
                balance = metrics.balance,
                currentOdometer = metrics.currentOdometer,
                dailyBudget = metrics.dailyBudget,
                isSafe = isSafe,
                isSyncPending = metrics.isSyncPending
            )

            logger.d("GetWidgetSummaryUseCase", "Widget summary calculated: balance=${summary.balance}")
            GetWidgetSummaryUseCase.Output.Success(summary)
        }
    }
}
