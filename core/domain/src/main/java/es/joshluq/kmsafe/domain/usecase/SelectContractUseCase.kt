package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Domain interface to select the active vehicle contract.
 */
interface SelectContractUseCase : FlowUseCase<SelectContractUseCase.Input, SelectContractUseCase.Output> {

    data class Input(
        val id: String,
        val minHoldDurationMs: Long = 700L
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        object Success : Output
    }
}

class SelectContractUseCaseImpl @Inject constructor(
    private val rentingRepository: RentingRepository,
    private val historyRepository: HistoryRepository,
    private val fuelRepository: FuelExpenseRepository,
    private val logger: LoggerKit
) : SelectContractUseCase {

    override fun invoke(input: SelectContractUseCase.Input): Flow<SelectContractUseCase.Output> = flow {
        emit(SelectContractUseCase.Output.Progress)
        val startTime = System.currentTimeMillis()
        logger.d("SelectContractUseCase", "Selecting contract ID: ${input.id}")

        rentingRepository.selectContract(input.id).first()

        // After selecting, wait for background sync of history and fuel expenses for this vehicle
        coroutineScope {
            val historyDeferred = async { historyRepository.syncHistory(input.id).first() }
            val fuelDeferred = async { fuelRepository.syncFuelExpenses(input.id).first() }
            historyDeferred.await()
            fuelDeferred.await()
        }

        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < input.minHoldDurationMs) {
            delay(input.minHoldDurationMs - elapsed)
        }

        logger.i("SelectContractUseCase", "Selection, history, and fuel expenses sync updated")
        emit(SelectContractUseCase.Output.Success)
    }.catch {
        logger.e("SelectContractUseCase", "Failed to update selection", it)
        emit(SelectContractUseCase.Output.Failure)
    }
}
