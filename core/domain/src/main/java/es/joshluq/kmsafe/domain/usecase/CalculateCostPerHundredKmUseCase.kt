package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Domain interface to compute dynamic €/100 km cost and fuel consumption performance metrics.
 */
interface CalculateCostPerHundredKmUseCase :
    FlowUseCase<CalculateCostPerHundredKmUseCase.Input, CalculateCostPerHundredKmUseCase.Output> {

    data class Input(val vehicleId: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(
            val costPer100km: Double?,
            val lastCycleConsumption: Double?,
            val averageConsumption: Double?,
            val consumptionDeltaVsAverage: Double?,
            val totalFullTankKms: Double,
            val totalFullTankCost: Double
        ) : Output
    }
}

class CalculateCostPerHundredKmUseCaseImpl @Inject constructor(
    private val expenseRepository: FuelExpenseRepository,
    private val logger: LoggerKit
) : CalculateCostPerHundredKmUseCase {

    override fun invoke(input: CalculateCostPerHundredKmUseCase.Input): Flow<CalculateCostPerHundredKmUseCase.Output> {
        logger.d("CalculateCostPerHundredKmUseCase", "Computing cost per 100km for vehicle: ${input.vehicleId}")
        return expenseRepository.getExpensesByVehicle(input.vehicleId).map { expenses ->
            val validFullTankExpenses = expenses.filter {
                it.isFullTank && (it.kmSinceLastRefuel ?: 0.0) > 0.0
            }

            if (validFullTankExpenses.isEmpty()) {
                return@map CalculateCostPerHundredKmUseCase.Output.Success(
                    costPer100km = null,
                    lastCycleConsumption = null,
                    averageConsumption = null,
                    consumptionDeltaVsAverage = null,
                    totalFullTankKms = 0.0,
                    totalFullTankCost = 0.0
                )
            }

            val totalFullTankKms = validFullTankExpenses.sumOf { it.kmSinceLastRefuel ?: 0.0 }
            val totalFullTankCost = validFullTankExpenses.sumOf { it.totalCost }
            val costPer100km = if (totalFullTankKms > 0.0) {
                (totalFullTankCost / totalFullTankKms) * 100.0
            } else {
                null
            }

            val lastExpense = validFullTankExpenses.maxByOrNull { it.timestamp }
            val lastCycleConsumption = lastExpense?.consumptionPer100km

            val consumptions = validFullTankExpenses.mapNotNull { it.consumptionPer100km }
            val averageConsumption = if (consumptions.isNotEmpty()) consumptions.average() else null

            val delta = if (lastCycleConsumption != null && averageConsumption != null) {
                lastCycleConsumption - averageConsumption
            } else {
                null
            }

            logger.i(
                "CalculateCostPerHundredKmUseCase",
                "Computed: costPer100km=$costPer100km, lastCycle=$lastCycleConsumption, avg=$averageConsumption"
            )

            CalculateCostPerHundredKmUseCase.Output.Success(
                costPer100km = costPer100km,
                lastCycleConsumption = lastCycleConsumption,
                averageConsumption = averageConsumption,
                consumptionDeltaVsAverage = delta,
                totalFullTankKms = totalFullTankKms,
                totalFullTankCost = totalFullTankCost
            )
        }.catch { e ->
            logger.e("CalculateCostPerHundredKmUseCase", "Error calculating cost per 100km", e)
            emit(
                CalculateCostPerHundredKmUseCase.Output.Success(
                    costPer100km = null,
                    lastCycleConsumption = null,
                    averageConsumption = null,
                    consumptionDeltaVsAverage = null,
                    totalFullTankKms = 0.0,
                    totalFullTankCost = 0.0
                )
            )
        }
    }
}
