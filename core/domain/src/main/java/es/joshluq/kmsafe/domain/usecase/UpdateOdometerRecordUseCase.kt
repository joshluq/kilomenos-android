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

/**
 * Domain interface to update an odometer record.
 */
interface UpdateOdometerRecordUseCase : FlowUseCase<UpdateOdometerRecordUseCase.Input, UpdateOdometerRecordUseCase.Output> {

    data class Input(val record: OdometerRecord) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        data class Failure(val message: String) : Output
        object Success : Output
    }
}

class UpdateOdometerRecordUseCaseImpl @Inject constructor(
    private val repository: HistoryRepository
) : UpdateOdometerRecordUseCase {

    override fun invoke(input: UpdateOdometerRecordUseCase.Input): Flow<UpdateOdometerRecordUseCase.Output> = flow {
        repository.updateRecord(input.record)
        emit(UpdateOdometerRecordUseCase.Output.Success as UpdateOdometerRecordUseCase.Output)
    }
        .onStart { emit(UpdateOdometerRecordUseCase.Output.Progress) }
        .catch { emit(UpdateOdometerRecordUseCase.Output.Failure(it.message ?: "Unknown error")) }
}
