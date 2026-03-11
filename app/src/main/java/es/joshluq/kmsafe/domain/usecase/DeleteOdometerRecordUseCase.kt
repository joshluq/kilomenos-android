package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class DeleteOdometerRecordUseCase @Inject constructor(
    private val repository: HistoryRepository,
    private val logger: LoggerKit
) : FlowUseCase<DeleteOdometerRecordUseCase.Input, DeleteOdometerRecordUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> = flow {
        logger.d("DeleteOdometerRecordUseCase", "Deleting record ID: ${input.record.id}")
        repository.deleteRecord(input.record)
        logger.i("DeleteOdometerRecordUseCase", "Record deleted")
        emit(Output.Success as Output)
    }
        .onStart { emit(Output.Progress) }
        .catch {
            logger.e("DeleteOdometerRecordUseCase", "Error deleting record", it)
            emit(Output.Failure)
        }

    data class Input(val record: OdometerRecord) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        object Success : Output
    }
}
