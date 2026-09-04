package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

/**
 * Domain interface to predict the cheapest day of the week to refuel at a specific station.
 */
interface PredictBestRefuelDayUseCase :
    FlowUseCase<PredictBestRefuelDayUseCase.Input, PredictBestRefuelDayUseCase.Output> {

    data class Input(val stationId: String, val fuelType: FuelType) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(
            val bestDayOfWeek: Int?,
            val bestDayAveragePrice: Double?,
            val overallAveragePrice: Double?,
            val savingPerUnit: Double?
        ) : Output
    }
}

class PredictBestRefuelDayUseCaseImpl @Inject constructor(
    private val expenseRepository: FuelExpenseRepository,
    private val logger: LoggerKit
) : PredictBestRefuelDayUseCase {

    override fun invoke(input: PredictBestRefuelDayUseCase.Input): Flow<PredictBestRefuelDayUseCase.Output> {
        logger.d("PredictBestRefuelDayUseCase", "Predicting best day for station: ${input.stationId}, fuel: ${input.fuelType}")
        return expenseRepository.getExpensesByStation(input.stationId).map { expenses ->
            val matching = expenses.filter { it.fuelType == input.fuelType && it.unitPrice > 0.0 }

            if (matching.size < 3) {
                return@map PredictBestRefuelDayUseCase.Output.Success(
                    bestDayOfWeek = null,
                    bestDayAveragePrice = null,
                    overallAveragePrice = null,
                    savingPerUnit = null
                )
            }

            val calendar = Calendar.getInstance()
            val pricesByDay = matching.groupBy { expense ->
                calendar.timeInMillis = expense.timestamp
                calendar.get(Calendar.DAY_OF_WEEK)
            }

            val averagesByDay = pricesByDay.mapValues { (_, entries) ->
                entries.map { it.unitPrice }.average()
            }

            val bestEntry = averagesByDay.minByOrNull { it.value }
            val overallAverage = matching.map { it.unitPrice }.average()

            val saving = if (bestEntry != null && bestEntry.value < overallAverage) {
                overallAverage - bestEntry.value
            } else {
                null
            }

            logger.i(
                "PredictBestRefuelDayUseCase",
                "Best day: ${bestEntry?.key}, price=${bestEntry?.value}, saving=$saving"
            )

            PredictBestRefuelDayUseCase.Output.Success(
                bestDayOfWeek = bestEntry?.key,
                bestDayAveragePrice = bestEntry?.value,
                overallAveragePrice = overallAverage,
                savingPerUnit = saving
            )
        }.catch { e ->
            logger.e("PredictBestRefuelDayUseCase", "Error predicting best day", e)
            emit(
                PredictBestRefuelDayUseCase.Output.Success(
                    bestDayOfWeek = null,
                    bestDayAveragePrice = null,
                    overallAveragePrice = null,
                    savingPerUnit = null
                )
            )
        }
    }
}
