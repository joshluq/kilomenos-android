package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.SyncStatus
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class GetOverviewDataUseCase @Inject constructor(
    private val rentingRepository: RentingRepository,
    private val historyRepository: HistoryRepository,
    private val logger: LoggerKit
) : FlowUseCase<GetOverviewDataUseCase.Input, GetOverviewDataUseCase.Output> {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: Input): Flow<Output> {
        logger.d("GetOverviewDataUseCase", "Executing")
        return rentingRepository.getContract().distinctUntilChanged().flatMapLatest { contract ->
            if (contract == null) {
                logger.w("GetOverviewDataUseCase", "No active contract found")
                return@flatMapLatest flowOf(Output.Success(null, 0.0) as Output)
            }

            historyRepository.getHistory(contract.id).distinctUntilChanged().map { records ->
                val totalKmsDriven = records.filter { !it.isInitialRecord }
                    .sumOf { it.odometerValue }
                    .toDouble()

                val pendingRecords = records.filter { it.syncStatus == SyncStatus.PENDING }
                val hasPendingRecords = pendingRecords.isNotEmpty()
                val isSyncPending = hasPendingRecords || contract.syncStatus == SyncStatus.PENDING

                if (isSyncPending) {
                    logger.i(
                        "GetOverviewDataUseCase",
                        "Sync Pending for ${contract.vehicleName}. " +
                            "Pending Records: ${pendingRecords.size}, Contract Pending: ${contract.syncStatus == SyncStatus.PENDING}"
                    )
                } else {
                    logger.d("GetOverviewDataUseCase", "Overview data synced for ${contract.vehicleName}")
                }
                
                Output.Success(contract, totalKmsDriven.coerceAtLeast(0.0), isSyncPending) as Output
            }
        }
            .distinctUntilChanged()
            .onStart { emit(Output.Progress) }
            .catch {
                logger.e("GetOverviewDataUseCase", "Error fetching overview data", it)
                emit(Output.Failure)
            }
    }

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        data class Success(
            val contract: RentingContract?,
            val actualKmsDrivenSinceStart: Double,
            val isSyncPending: Boolean = false
        ) : Output
    }
}
