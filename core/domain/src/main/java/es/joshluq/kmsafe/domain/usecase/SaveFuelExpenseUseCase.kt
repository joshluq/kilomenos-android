package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.FuelExpense
import es.joshluq.kmsafe.domain.model.FuelType
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import java.util.UUID
import javax.inject.Inject

/**
 * Use case for validating and saving a fuel or electric charging expense entry.
 *
 * Implements the **Hybrid A+C consumption algorithm**: calculates [FuelExpense.kmSinceLastRefuel]
 * by summing [es.joshluq.kmsafe.domain.model.OdometerRecord] values recorded between the previous
 * full-tank refuel and the current one. [FuelExpense.consumptionPer100km] is then derived from
 * `(volumeQuantity / kmSinceLastRefuel) * 100` and is only populated when [Input.isFullTank] is true
 * and sufficient historical data exists.
 */
class SaveFuelExpenseUseCase @Inject constructor(
    private val expenseRepository: FuelExpenseRepository,
    private val historyRepository: HistoryRepository,
    private val logger: LoggerKit
) : FlowUseCase<SaveFuelExpenseUseCase.Input, SaveFuelExpenseUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> = flow {
        logger.d("SaveFuelExpenseUseCase", "UseCase invoked for vehicle: ${input.vehicleId}, fullTank: ${input.isFullTank}")
        emit(Output.Progress)

        val isPriceReport = input.volumeQuantity == 0.0 && input.totalCost == 0.0 && input.unitPrice > 0.0

        if (!isPriceReport && (input.volumeQuantity <= 0.0 || input.unitPrice <= 0.0 || input.totalCost <= 0.0)) {
            logger.w("SaveFuelExpenseUseCase", "Invalid numeric values in expense input: volume=${input.volumeQuantity}, price=${input.unitPrice}, total=${input.totalCost}")
            emit(Output.InvalidInput(KmError.InvalidFuelExpenseValues))
            return@flow
        }

        // Hybrid A+C: infer km driven since last full refuel from OdometerRecord history
        val (kmSinceLastRefuel, consumptionPer100km) = calculateConsumption(input)

        val expenseId = input.id ?: UUID.randomUUID().toString()
        val expense = FuelExpense(
            id = expenseId,
            vehicleId = input.vehicleId,
            stationId = input.stationId,
            stationName = input.stationName,
            timestamp = input.timestamp ?: System.currentTimeMillis(),
            fuelType = input.fuelType,
            unitPrice = input.unitPrice,
            volumeQuantity = input.volumeQuantity,
            totalCost = input.totalCost,
            odometerAtExpense = input.odometerAtExpense,
            isFullTank = input.isFullTank,
            notes = input.notes,
            kmSinceLastRefuel = kmSinceLastRefuel,
            consumptionPer100km = consumptionPer100km
        )

        if (input.id == null) {
            expenseRepository.saveExpense(expense).first()
            logger.i("SaveFuelExpenseUseCase", "Expense created with ID: $expenseId")
        } else {
            expenseRepository.updateExpense(expense).first()
            logger.i("SaveFuelExpenseUseCase", "Expense updated with ID: $expenseId")
        }
        
        emit(Output.Success(expenseId))
    }
        .onStart { /* Progress already emitted inside flow block */ }
        .catch { e ->
            logger.e("SaveFuelExpenseUseCase", "Error saving expense", e)
            val error = (e as? KmException)?.error ?: KmError.UnknownError
            emit(Output.Failure(error))
        }

    /**
     * Calculates [FuelExpense.kmSinceLastRefuel] and [FuelExpense.consumptionPer100km] using
     * OdometerRecords as the ground truth for distance driven between refuels.
     *
     * @return Pair(kmSinceLastRefuel, consumptionPer100km). Both null if data is insufficient.
     */
    private suspend fun calculateConsumption(input: Input): Pair<Double?, Double?> {
        val lastRefuelTs = input.lastRefuelTimestamp ?: return Pair(null, null)

        return try {
            val records = historyRepository.getHistory(input.vehicleId).first()
            val currentTs = input.timestamp ?: System.currentTimeMillis()

            // Sum incremental km recorded between the previous full refuel and now
            val kmSinceLastRefuel = records
                .filter { record ->
                    !record.isInitialRecord &&
                        record.timestamp > lastRefuelTs &&
                        record.timestamp <= currentTs
                }
                .sumOf { it.odometerValue }

            if (kmSinceLastRefuel <= 0.0) {
                logger.d("SaveFuelExpenseUseCase", "No km recorded since last refuel, skipping consumption calculation")
                return Pair(null, null)
            }

            // Only compute consumption if this is a full-tank event
            val consumption = if (input.isFullTank) {
                (input.volumeQuantity / kmSinceLastRefuel) * 100.0
            } else {
                null
            }

            logger.d(
                "SaveFuelExpenseUseCase",
                "Consumption calculated: ${input.volumeQuantity}L / ${kmSinceLastRefuel}km = $consumption L/100km"
            )
            Pair(kmSinceLastRefuel, consumption)
        } catch (e: Exception) {
            logger.w("SaveFuelExpenseUseCase", "Failed to calculate consumption from history, defaulting to null", e)
            Pair(null, null)
        }
    }

    data class Input(
        val id: String? = null,
        val vehicleId: String,
        val stationId: String? = null,
        val stationName: String? = null,
        val timestamp: Long? = null,
        val fuelType: FuelType,
        val unitPrice: Double,
        val volumeQuantity: Double,
        val totalCost: Double,
        val odometerAtExpense: Double? = null,
        val isFullTank: Boolean = true,
        val notes: String? = null,
        /** Timestamp (epoch ms) of the previous [FuelExpense] where [isFullTank] was true.
         * Used to delimit the OdometerRecord window for the Hybrid A+C algorithm. */
        val lastRefuelTimestamp: Long? = null
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Failure(val error: KmError) : Output
        data class InvalidInput(val error: KmError) : Output
        data class Success(val expenseId: String) : Output
    }
}
