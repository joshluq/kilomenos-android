package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import java.util.Calendar
import javax.inject.Inject

/**
 * Use case to retrieve expenses for the active vehicle, calculating monthly totals and KPIs.
 *
 * In addition to expense aggregates, this use case resolves:
 * - [Output.Success.currentOdometer]: the real-time odometer (`startOdometer + ∑ odometerValues`) used to
 *   pre-fill the Add Expense form without requiring user input.
 * - [Output.Success.lastRefuelTimestamp]: timestamp of the last [FuelExpense] where `isFullTank = true`,
 *   used as the start boundary for the Hybrid A+C consumption algorithm in [SaveFuelExpenseUseCase].
 */
class GetExpensesByVehicleUseCase @Inject constructor(
    private val expenseRepository: FuelExpenseRepository,
    private val rentingRepository: RentingRepository,
    private val historyRepository: HistoryRepository,
    private val logger: LoggerKit
) : FlowUseCase<GetExpensesByVehicleUseCase.Input, GetExpensesByVehicleUseCase.Output> {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: Input): Flow<Output> {
        logger.d("GetExpensesByVehicleUseCase", "Invoking for vehicleId: ${input.vehicleId ?: "Active"}")

        val vehicleFlow = if (input.vehicleId != null) {
            rentingRepository.getContractById(input.vehicleId)
        } else {
            rentingRepository.getContract()
        }

        return vehicleFlow.flatMapLatest { contract ->
            if (contract == null) {
                logger.w("GetExpensesByVehicleUseCase", "No vehicle found")
                return@flatMapLatest flowOf(Output.Failure(KmError.UnknownError))
            }

            val expensesFlow = expenseRepository.getExpensesByVehicle(contract.id)
            val historyFlow = historyRepository.getHistory(contract.id)

            combine(expensesFlow, historyFlow) { allExpenses, records ->
                // Filter out price reports (non-real expenses) for list and totals
                val realExpenses = allExpenses.filter { it.volumeQuantity > 0.0 }
                
                // Compute real-time odometer: startOdometer + sum of all increments
                val totalKmDriven = records.filter { !it.isInitialRecord }.sumOf { it.odometerValue }
                val currentOdometer = contract.startOdometer + totalKmDriven

                // Find last full-tank refuel timestamp for the A+C consumption window
                val lastFullRefuel = realExpenses.filter { it.isFullTank }.maxByOrNull { it.timestamp }
                val lastRefuelTimestamp = lastFullRefuel?.timestamp

                // Compute km driven since that last full refuel and get associated records
                val recordsSinceLastRefuel = if (lastRefuelTimestamp != null) {
                    records.filter { !it.isInitialRecord && it.timestamp > lastRefuelTimestamp }
                        .sortedByDescending { it.timestamp }
                } else {
                    emptyList()
                }

                val kmSinceLastFullRefuel = if (lastRefuelTimestamp != null) {
                    recordsSinceLastRefuel.sumOf { it.odometerValue }
                } else {
                    null
                }

                if (realExpenses.isEmpty()) {
                    Output.Empty(
                        vehicleId = contract.id,
                        vehicleName = contract.vehicleName,
                        currentOdometer = currentOdometer,
                        lastRefuelTimestamp = lastRefuelTimestamp,
                        kmSinceLastFullRefuel = kmSinceLastFullRefuel,
                        recordsSinceLastRefuel = recordsSinceLastRefuel,
                        defaultFuelType = contract.fuelType
                    )
                } else {
                    val sortedExpenses = realExpenses.sortedByDescending { it.timestamp }
                    val currentMonthExpenses = filterCurrentMonthExpenses(sortedExpenses)

                    val totalSpentCurrentMonth = currentMonthExpenses.sumOf { it.totalCost }
                    val totalLitersOrKwhCurrentMonth = currentMonthExpenses.sumOf { it.volumeQuantity }
                    val totalSpentAllTime = sortedExpenses.sumOf { it.totalCost }

                    Output.Success(
                        vehicleId = contract.id,
                        vehicleName = contract.vehicleName,
                        expenses = sortedExpenses,
                        currentMonthTotalCost = totalSpentCurrentMonth,
                        currentMonthTotalVolume = totalLitersOrKwhCurrentMonth,
                        allTimeTotalCost = totalSpentAllTime,
                        currentOdometer = currentOdometer,
                        lastRefuelTimestamp = lastRefuelTimestamp,
                        kmSinceLastFullRefuel = kmSinceLastFullRefuel,
                        recordsSinceLastRefuel = recordsSinceLastRefuel,
                        defaultFuelType = contract.fuelType
                    )
                }
            }
        }
            .onStart { emit(Output.Progress) }
            .catch { e ->
                logger.e("GetExpensesByVehicleUseCase", "Error loading expenses", e)
                val error = (e as? KmException)?.error ?: KmError.UnknownError
                emit(Output.Failure(error))
            }
    }

    private fun filterCurrentMonthExpenses(expenses: List<FuelExpense>): List<FuelExpense> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return expenses.filter { expense ->
            val expenseCal = Calendar.getInstance().apply { timeInMillis = expense.timestamp }
            expenseCal.get(Calendar.MONTH) == currentMonth && expenseCal.get(Calendar.YEAR) == currentYear
        }
    }

    data class Input(val vehicleId: String? = null) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Failure(val error: KmError) : Output
        data class Empty(
            val vehicleId: String,
            val vehicleName: String,
            /** Real-time odometer for pre-filling the Add Expense form. */
            val currentOdometer: Double,
            /** Timestamp of the last full-tank refuel, or null if no full refuel exists yet. */
            val lastRefuelTimestamp: Long?,
            val kmSinceLastFullRefuel: Double?,
            val recordsSinceLastRefuel: List<OdometerRecord>,
            val defaultFuelType: FuelType
        ) : Output
        data class Success(
            val vehicleId: String,
            val vehicleName: String,
            val expenses: List<FuelExpense>,
            val currentMonthTotalCost: Double,
            val currentMonthTotalVolume: Double,
            val allTimeTotalCost: Double,
            /** Real-time odometer for pre-filling the Add Expense form. */
            val currentOdometer: Double,
            /** Timestamp of the last full-tank refuel, or null if no full refuel exists yet. */
            val lastRefuelTimestamp: Long?,
            val kmSinceLastFullRefuel: Double?,
            val recordsSinceLastRefuel: List<OdometerRecord>,
            val defaultFuelType: FuelType
        ) : Output
    }
}
