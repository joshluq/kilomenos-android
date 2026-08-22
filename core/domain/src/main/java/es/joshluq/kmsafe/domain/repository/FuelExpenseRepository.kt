package es.joshluq.kmsafe.domain.repository

import es.joshluq.kmsafe.domain.model.FuelExpense
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for fuel and EV energy expenses.
 *
 * Follows clean architecture principles with zero Android platform dependencies.
 */
interface FuelExpenseRepository {

    /**
     * Observes all expenses recorded for the given vehicle contract ID.
     */
    fun getExpensesByVehicle(vehicleId: String): Flow<List<FuelExpense>>

    /**
     * Observes all expenses linked to a specific service station.
     */
    fun getExpensesByStation(stationId: String): Flow<List<FuelExpense>>

    /**
     * Observes a specific expense record by its ID.
     */
    fun getExpenseById(id: String): Flow<FuelExpense?>

    /**
     * Saves or updates a fuel expense locally and schedules sync if applicable.
     *
     * @return Flow emitting the saved expense ID.
     */
    fun saveExpense(expense: FuelExpense): Flow<String>

    /**
     * Updates an existing fuel expense locally and schedules sync if applicable.
     */
    fun updateExpense(expense: FuelExpense): Flow<Unit>

    /**
     * Deletes an expense entry by ID.
     */
    fun deleteExpense(id: String): Flow<Unit>

    /**
     * Clears all expense records associated with a specific vehicle contract.
     */
    fun deleteExpensesByVehicle(vehicleId: String): Flow<Unit>

    /**
     * Clears all fuel expenses from the database.
     */
    fun clearAllExpenses(): Flow<Unit>

    /**
     * Synchronizes fuel expenses with the remote server.
     */
    fun syncFuelExpenses(vehicleId: String): Flow<List<FuelExpense>>
}
