package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.ServiceStationDetail
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Use case to retrieve detailed information, statistics, and history for a specific service station.
 */
class GetServiceStationDetailUseCase @Inject constructor(
    private val stationRepository: ServiceStationRepository,
    private val expenseRepository: FuelExpenseRepository,
    private val logger: LoggerKit
) : FlowUseCase<GetServiceStationDetailUseCase.Input, GetServiceStationDetailUseCase.Output> {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun invoke(input: Input): Flow<Output> {
        logger.d("GetServiceStationDetail", "Fetching detail for station: ${input.stationId}")
        
        return stationRepository.getStationById(input.stationId).flatMapLatest { station ->
            if (station == null) {
                return@flatMapLatest flowOf(Output.Failure(KmError.UnknownError))
            }

            // We combine the station info with all expenses linked to it
            expenseRepository.getExpensesByStation(station.id)
                .combine(flowOf(station)) { stationExpenses, s ->
                    val detail = ServiceStationDetail(
                        station = s,
                        totalSpent = stationExpenses.sumOf { it.totalCost },
                        totalVolume = stationExpenses.sumOf { it.volumeQuantity },
                        refuelCount = stationExpenses.size,
                        expenseHistory = stationExpenses.sortedByDescending { it.timestamp },
                        averageConsumption = stationExpenses
                            .mapNotNull { it.consumptionPer100km }
                            .takeIf { it.isNotEmpty() }
                            ?.average()
                    )
                    Output.Success(detail)
                }
        }
        .onStart { emit(Output.Progress) }
        .catch { e ->
            logger.e("GetServiceStationDetail", "Error fetching station detail", e)
            emit(Output.Failure(KmError.UnknownError))
        }
    }

    data class Input(val stationId: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Failure(val error: KmError) : Output
        data class Success(val detail: ServiceStationDetail) : Output
    }
}
