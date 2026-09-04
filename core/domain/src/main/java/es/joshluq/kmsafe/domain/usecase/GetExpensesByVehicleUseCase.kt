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
 * Domain interface to retrieve expenses for the active vehicle, calculating monthly totals and KPIs.
 */
interface GetExpensesByVehicleUseCase : FlowUseCase<GetExpensesByVehicleUseCase.Input, GetExpensesByVehicleUseCase.Output> {

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

class GetExpensesByVehicleUseCaseImpl @Inject constructor(
    private val expenseRepository: FuelExpenseRepository,
    private val rentingRepository: RentingRepository,
    private val historyRepository: HistoryRepository,
    private val logger: LoggerKit
) : GetExpensesByVehicleUseCase {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: GetExpensesByVehicleUseCase.Input): Flow<GetExpensesByVehicleUseCase.Output> {
        logger.d("GetExpensesByVehicleUseCase", "Invoking for vehicleId: ${input.vehicleId ?: "Active"}")

        val vehicleFlow = if (input.vehicleId != null) {
            rentingRepository.getContractById(input.vehicleId)
        } else {
            rentingRepository.getContract()
        }

        return vehicleFlow.flatMapLatest { contract ->
            if (contract == null) {
                logger.w("GetExpensesByVehicleUseCase", "No vehicle found")
                return@flatMapLatest flowOf(GetExpensesByVehicleUseCase.Output.Failure(KmError.UnknownError))
            }

            val expensesFlow = expenseRepository.getExpensesByVehicle(contract.id)
            val historyFlow = historyRepository.getHistory(contract.id)

            combine(expensesFlow, historyFlow) { allExpenses, records ->
                val (currentOdometer, lastFullRefuelTs, kmSinceLast, recordsSince) =
                    computeOdometerAndRefuelWindow(contract.startOdometer, records, allExpenses)

                if (allExpenses.isEmpty()) {
                    logger.d("GetExpensesByVehicleUseCase", "No expenses found for vehicle: ${contract.id}")
                    return@combine GetExpensesByVehicleUseCase.Output.Empty(
                        vehicleId = contract.id,
                        vehicleName = contract.vehicleName,
                        currentOdometer = currentOdometer,
                        lastRefuelTimestamp = lastFullRefuelTs,
                        kmSinceLastFullRefuel = kmSinceLast,
                        recordsSinceLastRefuel = recordsSince,
                        defaultFuelType = contract.fuelType
                    )
                }

                val currentMonthExpenses = filterCurrentMonthExpenses(allExpenses)
                val currentMonthTotalCost = currentMonthExpenses.sumOf { it.totalCost }
                val currentMonthTotalVolume = currentMonthExpenses.sumOf { it.volumeQuantity }
                val allTimeTotalCost = allExpenses.sumOf { it.totalCost }

                logger.d(
                    "GetExpensesByVehicleUseCase",
                    "Expenses calculated: currentMonth=$currentMonthTotalCost, allTime=$allTimeTotalCost, odometer=$currentOdometer"
                )

                GetExpensesByVehicleUseCase.Output.Success(
                    vehicleId = contract.id,
                    vehicleName = contract.vehicleName,
                    expenses = allExpenses,
                    currentMonthTotalCost = currentMonthTotalCost,
                    currentMonthTotalVolume = currentMonthTotalVolume,
                    allTimeTotalCost = allTimeTotalCost,
                    currentOdometer = currentOdometer,
                    lastRefuelTimestamp = lastFullRefuelTs,
                    kmSinceLastFullRefuel = kmSinceLast,
                    recordsSinceLastRefuel = recordsSince,
                    defaultFuelType = contract.fuelType
                )
            }
        }
            .onStart { emit(GetExpensesByVehicleUseCase.Output.Progress) }
            .catch { e ->
                logger.e("GetExpensesByVehicleUseCase", "Error loading expenses", e)
                val error = (e as? KmException)?.error ?: KmError.UnknownError
                emit(GetExpensesByVehicleUseCase.Output.Failure(error))
            }
    }

    private data class RefuelWindowData(
        val currentOdometer: Double,
        val lastRefuelTimestamp: Long?,
        val kmSinceLastFullRefuel: Double?,
        val recordsSinceLastRefuel: List<OdometerRecord>
    )

    private fun computeOdometerAndRefuelWindow(
        startOdometer: Double,
        records: List<OdometerRecord>,
        expenses: List<FuelExpense>
    ): RefuelWindowData {
        val totalKilometersDriven = records
            .filter { !it.isInitialRecord }
            .sumOf { it.odometerValue }
        val currentOdometer = startOdometer + totalKilometersDriven

        val lastFullRefuel = expenses
            .filter { it.isFullTank }
            .maxByOrNull { it.timestamp }

        val lastFullRefuelTs = lastFullRefuel?.timestamp

        val (kmSince, recordsSince) = if (lastFullRefuelTs != null) {
            val relevantRecords = records.filter { it.timestamp >= lastFullRefuelTs }
            val km = relevantRecords.filter { !it.isInitialRecord }.sumOf { it.odometerValue }
            Pair(km, relevantRecords)
        } else {
            Pair(null, emptyList())
        }

        return RefuelWindowData(currentOdometer, lastFullRefuelTs, kmSince, recordsSince)
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
}
