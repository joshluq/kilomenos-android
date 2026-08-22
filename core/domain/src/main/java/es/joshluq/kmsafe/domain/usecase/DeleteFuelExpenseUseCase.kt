package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Use case to delete a fuel expense entry.
 */
class DeleteFuelExpenseUseCase @Inject constructor(
    private val expenseRepository: FuelExpenseRepository,
    private val logger: LoggerKit
) : FlowUseCase<DeleteFuelExpenseUseCase.Input, DeleteFuelExpenseUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> {
        logger.d("DeleteFuelExpenseUseCase", "Deleting expense ID: ${input.expenseId}")

        return expenseRepository.deleteExpense(input.expenseId)
            .map {
                logger.i("DeleteFuelExpenseUseCase", "Expense deleted: ${input.expenseId}")
                Output.Success as Output
            }
            .onStart { emit(Output.Progress) }
            .catch { e ->
                logger.e("DeleteFuelExpenseUseCase", "Error deleting expense", e)
                val error = (e as? KmException)?.error ?: KmError.UnknownError
                emit(Output.Failure(error))
            }
    }

    data class Input(val expenseId: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Failure(val error: KmError) : Output
        data object Success : Output
    }
}
