package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.EnergyCategory
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Use case to compute estimated electrification savings (Cost in € of kWh vs equivalent gasoline cost).
 */
class GetElectrificationSavingsUseCase @Inject constructor(
    private val expenseRepository: FuelExpenseRepository,
    private val logger: LoggerKit
) : FlowUseCase<GetElectrificationSavingsUseCase.Input, GetElectrificationSavingsUseCase.Output> {

    companion object {
        // Average benchmark: ~6.5L/100km for gasoline at standard average price
        private const val BENCHMARK_GASOLINE_PRICE_PER_LITER = 1.65
        private const val BENCHMARK_GASOLINE_LITERS_PER_100KM = 6.5
        // Benchmark EV: ~17 kWh/100km
        private const val BENCHMARK_EV_KWH_PER_100KM = 17.0
    }

    override fun invoke(input: Input): Flow<Output> {
        logger.d("GetElectrificationSavingsUseCase", "Calculating electrification savings for: ${input.vehicleId}")

        return expenseRepository.getExpensesByVehicle(input.vehicleId).map { expenses ->
            val electricExpenses = expenses.filter { it.fuelType.category == EnergyCategory.ELECTRIC }

            if (electricExpenses.isEmpty()) {
                Output.Empty
            } else {
                val totalKwhConsumed = electricExpenses.sumOf { it.volumeQuantity }
                val totalElectricCost = electricExpenses.sumOf { it.totalCost }

                // Estimated equivalent km driven on electricity
                val equivalentKmDriven = (totalKwhConsumed / BENCHMARK_EV_KWH_PER_100KM) * 100.0

                // Theoretical cost if those km were driven with gasoline
                val theoreticalGasolineLiters = (equivalentKmDriven / 100.0) * BENCHMARK_GASOLINE_LITERS_PER_100KM
                val theoreticalGasolineCost = theoreticalGasolineLiters * BENCHMARK_GASOLINE_PRICE_PER_LITER

                val totalSavedEuros = (theoreticalGasolineCost - totalElectricCost).coerceAtLeast(0.0)

                Output.Success(
                    totalKwh = totalKwhConsumed,
                    totalElectricCost = totalElectricCost,
                    theoreticalGasolineCost = theoreticalGasolineCost,
                    totalSavedEuros = totalSavedEuros,
                    equivalentKmDriven = equivalentKmDriven
                )
            }
        }
            .onStart { emit(Output.Progress) }
            .catch { e ->
                logger.e("GetElectrificationSavingsUseCase", "Error computing electrification savings", e)
                emit(Output.Failure)
            }
    }

    data class Input(val vehicleId: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data object Empty : Output
        data class Success(
            val totalKwh: Double,
            val totalElectricCost: Double,
            val theoreticalGasolineCost: Double,
            val totalSavedEuros: Double,
            val equivalentKmDriven: Double
        ) : Output
    }
}
