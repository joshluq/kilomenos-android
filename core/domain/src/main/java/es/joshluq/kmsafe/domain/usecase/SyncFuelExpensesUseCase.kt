package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Domain interface to trigger a synchronization of vehicle fuel/energy expenses from the remote server.
 */
interface SyncFuelExpensesUseCase : FlowUseCase<SyncFuelExpensesUseCase.Input, SyncFuelExpensesUseCase.Output> {

    data class Input(val vehicleId: String? = null) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Success(val expensesCount: Int) : Output
        data class Failure(val error: KmError) : Output
    }
}

class SyncFuelExpensesUseCaseImpl @Inject constructor(
    private val fuelExpenseRepository: FuelExpenseRepository,
    private val rentingRepository: RentingRepository,
    private val logger: LoggerKit
) : SyncFuelExpensesUseCase {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: SyncFuelExpensesUseCase.Input): Flow<SyncFuelExpensesUseCase.Output> {
        logger.d("SyncFuelExpensesUseCase", "Triggering fuel expenses sync (vehicleId=${input.vehicleId ?: "Active"})")

        val vehicleFlow = if (input.vehicleId != null) {
            rentingRepository.getContractById(input.vehicleId)
        } else {
            rentingRepository.getContract()
        }

        return vehicleFlow.flatMapLatest { contract ->
            if (contract == null) {
                logger.w("SyncFuelExpensesUseCase", "No vehicle contract found for sync")
                return@flatMapLatest flowOf(SyncFuelExpensesUseCase.Output.Failure(KmError.UnknownError))
            }

            fuelExpenseRepository.syncFuelExpenses(contract.id)
                .map { expenses ->
                    logger.i("SyncFuelExpensesUseCase", "Sync completed successfully: ${expenses.size} expenses")
                    SyncFuelExpensesUseCase.Output.Success(expenses.size) as SyncFuelExpensesUseCase.Output
                }
        }
            .onStart { emit(SyncFuelExpensesUseCase.Output.Progress) }
            .catch { e ->
                logger.e("SyncFuelExpensesUseCase", "Fuel expenses synchronization failed", e)
                val error = (e as? KmException)?.error ?: KmError.NetworkError
                emit(SyncFuelExpensesUseCase.Output.Failure(error))
            }
    }
}
