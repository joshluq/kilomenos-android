package es.joshluq.kmsafe.domain.usecase

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

class UpdateOdometerRecordUseCase @Inject constructor(
    private val repository: HistoryRepository
) : FlowUseCase<UpdateOdometerRecordUseCase.Input, UpdateOdometerRecordUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> = flow {
        repository.updateRecord(input.record)
        emit(Output.Success as Output)
    }
        .onStart { emit(Output.Progress) }
        .catch { emit(Output.Failure(it.message ?: "Unknown error")) }

    data class Input(val record: OdometerRecord) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        data class Failure(val message: String) : Output
        object Success : Output
    }
}
