package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Domain interface to fetch a single odometer record and its previous record.
 */
interface GetOdometerRecordUseCase : FlowUseCase<GetOdometerRecordUseCase.Input, GetOdometerRecordUseCase.Output> {

    data class Input(val recordId: String) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        data class Failure(val message: String) : Output
        data class Success(val record: OdometerRecord, val previousRecord: OdometerRecord?) : Output
    }
}

class GetOdometerRecordUseCaseImpl @Inject constructor(
    private val rentingRepository: RentingRepository,
    private val historyRepository: HistoryRepository
) : GetOdometerRecordUseCase {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: GetOdometerRecordUseCase.Input): Flow<GetOdometerRecordUseCase.Output> {
        return rentingRepository.getContract().flatMapLatest { contract ->
            if (contract == null) return@flatMapLatest flowOf(GetOdometerRecordUseCase.Output.Failure("No contract found"))

            historyRepository.getHistory(contract.id).map { records ->
                val sortedRecords = records.sortedBy { it.timestamp }
                val targetIndex = sortedRecords.indexOfFirst { it.id == input.recordId }

                if (targetIndex == -1) {
                    GetOdometerRecordUseCase.Output.Failure("Record not found")
                } else {
                    val target = sortedRecords[targetIndex]
                    val previous = if (targetIndex > 0) sortedRecords[targetIndex - 1] else null
                    GetOdometerRecordUseCase.Output.Success(target, previous)
                }
            }
        }.onStart { emit(GetOdometerRecordUseCase.Output.Progress) }
    }
}
