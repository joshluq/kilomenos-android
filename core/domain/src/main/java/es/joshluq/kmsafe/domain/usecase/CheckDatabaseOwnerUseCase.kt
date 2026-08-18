package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class CheckDatabaseOwnerUseCase @Inject constructor(
    private val repository: RentingRepository,
    private val logger: LoggerKit
) : FlowUseCase<CheckDatabaseOwnerUseCase.Input, CheckDatabaseOwnerUseCase.Output> {

    override fun invoke(input: Input): Flow<Output> = flow {
        logger.d("CheckDatabaseOwnerUseCase", "Checking database owner (One-shot)")
        val ownerId = repository.getDatabaseOwnerId()
        emit(Output.Success(ownerId) as Output)
    }.onStart { emit(Output.Progress) }

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        data class Success(val ownerId: String?) : Output
    }
}
