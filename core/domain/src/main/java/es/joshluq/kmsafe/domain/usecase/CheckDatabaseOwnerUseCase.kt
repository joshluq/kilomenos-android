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

/**
 * Domain interface to check the database owner.
 */
interface CheckDatabaseOwnerUseCase : FlowUseCase<CheckDatabaseOwnerUseCase.Input, CheckDatabaseOwnerUseCase.Output> {
    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        data class Success(val ownerId: String?) : Output
    }
}

class CheckDatabaseOwnerUseCaseImpl @Inject constructor(
    private val repository: RentingRepository,
    private val logger: LoggerKit
) : CheckDatabaseOwnerUseCase {

    override fun invoke(input: CheckDatabaseOwnerUseCase.Input): Flow<CheckDatabaseOwnerUseCase.Output> = flow {
        logger.d("CheckDatabaseOwnerUseCase", "Checking database owner (One-shot)")
        val ownerId = repository.getDatabaseOwnerId()
        emit(CheckDatabaseOwnerUseCase.Output.Success(ownerId) as CheckDatabaseOwnerUseCase.Output)
    }.onStart { emit(CheckDatabaseOwnerUseCase.Output.Progress) }
}
