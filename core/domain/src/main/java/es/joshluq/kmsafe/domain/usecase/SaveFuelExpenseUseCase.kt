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
 * Domain interface for validating and saving a fuel or electric charging expense entry.
 */
interface SaveFuelExpenseUseCase : FlowUseCase<SaveFuelExpenseUseCase.Input, SaveFuelExpenseUseCase.Output> {

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

class SaveFuelExpenseUseCaseImpl @Inject constructor(
    private val expenseRepository: FuelExpenseRepository,
    private val historyRepository: HistoryRepository,
    private val logger: LoggerKit
) : SaveFuelExpenseUseCase {

    override fun invoke(input: SaveFuelExpenseUseCase.Input): Flow<SaveFuelExpenseUseCase.Output> = flow {
        logger.d("SaveFuelExpenseUseCase", "UseCase invoked for vehicle: ${input.vehicleId}, fullTank: ${input.isFullTank}")
        emit(SaveFuelExpenseUseCase.Output.Progress)

        val isPriceReport = input.volumeQuantity == 0.0 && input.totalCost == 0.0 && input.unitPrice > 0.0

        if (!isPriceReport && (input.volumeQuantity <= 0.0 || input.unitPrice <= 0.0 || input.totalCost <= 0.0)) {
            logger.w("SaveFuelExpenseUseCase", "Invalid numeric values in expense input: volume=${input.volumeQuantity}, price=${input.unitPrice}, total=${input.totalCost}")
            emit(SaveFuelExpenseUseCase.Output.InvalidInput(KmError.InvalidFuelExpenseValues))
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
            kmSinceLastRefuel = kmSinceLastRefuel,
            consumptionPer100km = consumptionPer100km,
            notes = input.notes
        )

        logger.d("SaveFuelExpenseUseCase", "Saving expense: $expenseId with kmSinceRefuel=$kmSinceLastRefuel, consumption=$consumptionPer100km")

        val savedId = expenseRepository.saveExpense(expense).first()
        logger.i("SaveFuelExpenseUseCase", "Expense saved successfully: $savedId")
        emit(SaveFuelExpenseUseCase.Output.Success(savedId))
    }
        .onStart { emit(SaveFuelExpenseUseCase.Output.Progress) }
        .catch { e ->
            logger.e("SaveFuelExpenseUseCase", "Error saving expense", e)
            val error = (e as? KmException)?.error ?: KmError.UnknownError
            emit(SaveFuelExpenseUseCase.Output.Failure(error))
        }

    /**
     * Hybrid A+C algorithm:
     * - Queries [HistoryRepository] for odometer records belonging to [Input.vehicleId].
     * - If [Input.lastRefuelTimestamp] is provided, only sums records whose timestamp is > that boundary.
     * - Returns `(kmDriven, consumptionLPer100km)` only when [Input.isFullTank] is true and kmDriven > 0.
     * - Gracefully falls back to `(null, null)` on any error or missing data.
     */
    private suspend fun calculateConsumption(input: SaveFuelExpenseUseCase.Input): Pair<Double?, Double?> {
        if (!input.isFullTank) {
            logger.d("SaveFuelExpenseUseCase", "isFullTank=false, skipping consumption calculation")
            return Pair(null, null)
        }

        return try {
            val records = historyRepository.getHistory(input.vehicleId).first()

            val relevantRecords = if (input.lastRefuelTimestamp != null) {
                records.filter { it.timestamp > input.lastRefuelTimestamp }
            } else {
                records
            }

            val kmSinceLastRefuel = relevantRecords
                .filter { !it.isInitialRecord }
                .sumOf { it.odometerValue }

            if (kmSinceLastRefuel <= 0.0) {
                logger.d("SaveFuelExpenseUseCase", "No km accumulated in window (km=$kmSinceLastRefuel), skipping consumption")
                return Pair(null, null)
            }

            val consumption = (input.volumeQuantity / kmSinceLastRefuel) * 100.0
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
}
