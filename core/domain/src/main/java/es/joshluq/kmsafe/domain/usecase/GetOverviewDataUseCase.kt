package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.ContractMetrics
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Domain interface for retrieving aggregated overview data for the active contract.
 */
interface GetOverviewDataUseCase : FlowUseCase<GetOverviewDataUseCase.Input, GetOverviewDataUseCase.Output> {

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        data class Success(
            val contract: RentingContract?,
            val actualKmsDrivenSinceStart: Double,
            val isSyncPending: Boolean = false,
            val metrics: ContractMetrics? = null
        ) : Output
    }
}

class GetOverviewDataUseCaseImpl @Inject constructor(
    private val rentingRepository: RentingRepository,
    private val historyRepository: HistoryRepository,
    private val calculateContractMetricsUseCase: CalculateContractMetricsUseCase,
    private val logger: LoggerKit
) : GetOverviewDataUseCase {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: GetOverviewDataUseCase.Input): Flow<GetOverviewDataUseCase.Output> {
        logger.d("GetOverviewDataUseCase", "Executing")
        return rentingRepository.getContract().distinctUntilChanged().flatMapLatest { contract ->
            if (contract == null) {
                logger.w("GetOverviewDataUseCase", "No active contract found")
                return@flatMapLatest flowOf(GetOverviewDataUseCase.Output.Success(null, 0.0) as GetOverviewDataUseCase.Output)
            }

            historyRepository.getHistory(contract.id).distinctUntilChanged().map { records ->
                val metrics = calculateContractMetricsUseCase.calculate(contract, records)

                if (metrics.isSyncPending) {
                    logger.i(
                        "GetOverviewDataUseCase",
                        "Sync Pending for ${contract.vehicleName}."
                    )
                } else {
                    logger.d("GetOverviewDataUseCase", "Overview data synced for ${contract.vehicleName}")
                }

                GetOverviewDataUseCase.Output.Success(
                    contract = contract,
                    actualKmsDrivenSinceStart = metrics.actualKmsDriven,
                    isSyncPending = metrics.isSyncPending,
                    metrics = metrics
                ) as GetOverviewDataUseCase.Output
            }
        }
            .distinctUntilChanged()
            .onStart { emit(GetOverviewDataUseCase.Output.Progress) }
            .catch {
                logger.e("GetOverviewDataUseCase", "Error fetching overview data", it)
                emit(GetOverviewDataUseCase.Output.Failure)
            }
    }
}
