package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Domain interface to trigger a manual synchronization of service stations from the remote server.
 */
interface SyncStationsUseCase : FlowUseCase<SyncStationsUseCase.Input, SyncStationsUseCase.Output> {

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Success : Output
        data class Failure(val message: String) : Output
    }
}

class SyncStationsUseCaseImpl @Inject constructor(
    private val repository: ServiceStationRepository,
    private val logger: LoggerKit
) : SyncStationsUseCase {

    override fun invoke(input: SyncStationsUseCase.Input): Flow<SyncStationsUseCase.Output> {
        logger.d("SyncStationsUseCase", "Triggering station synchronization")
        return repository.syncStations()
            .map {
                SyncStationsUseCase.Output.Success as SyncStationsUseCase.Output
            }
            .onStart { emit(SyncStationsUseCase.Output.Progress) }
            .catch {
                logger.e("SyncStationsUseCase", "Station synchronization failed", it)
                emit(SyncStationsUseCase.Output.Failure(it.message ?: "Unknown error"))
            }
    }
}
