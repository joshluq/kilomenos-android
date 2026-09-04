package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RecordWithIndicator
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Domain interface to retrieve the odometer history grouped by month.
 */
interface GetHistoryUseCase : FlowUseCase<GetHistoryUseCase.Input, GetHistoryUseCase.Output> {

    data class Input(val forceRefresh: Boolean = false) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data object Empty : Output
        data class Success(
            val contractId: String,
            val initialRecord: OdometerRecord?,
            val allRecords: List<RecordWithIndicator>,
            val totalKms: Double,
            val totalRecordsCount: Int
        ) : Output
    }
}

class GetHistoryUseCaseImpl @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val rentingRepository: RentingRepository,
    private val logger: LoggerKit
) : GetHistoryUseCase {

    companion object {
        private const val DAYS_IN_MONTH = 30.4375
        private const val MILLIS_IN_DAY = 1000L * 60 * 60 * 24
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: GetHistoryUseCase.Input): Flow<GetHistoryUseCase.Output> {
        logger.d("GetHistoryUseCase", "Fetching history (forceRefresh=${input.forceRefresh})")
        return rentingRepository.getContract().flatMapLatest { contract ->
            if (contract == null) {
                logger.w("GetHistoryUseCase", "No active contract")
                return@flatMapLatest flowOf(GetHistoryUseCase.Output.Empty)
            }

            val localFlow = historyRepository.getHistory(contract.id)

            if (input.forceRefresh) {
                // If forceRefresh, we combine local data with the sync operation
                combine(
                    localFlow,
                    historyRepository.syncHistory(contract.id)
                        .catch { logger.e("GetHistoryUseCase", "Background sync failed", it) }
                ) { records, _ ->
                    processRecords(records, contract)
                }
            } else {
                // SSOT: Only local data
                localFlow.map { records ->
                    processRecords(records, contract)
                }
            }
        }.onStart { emit(GetHistoryUseCase.Output.Progress) }
    }

    private fun processRecords(records: List<OdometerRecord>, contract: RentingContract): GetHistoryUseCase.Output {
        if (records.isEmpty()) {
            return GetHistoryUseCase.Output.Empty
        }

        val initialRecord = records.find { it.isInitialRecord }

        // Core Business Rule: Exclude the initial contract record from the consumed real kilometers
        val sortedAscending = records.filter { !it.isInitialRecord }.sortedBy { it.timestamp }
        val totalKms = sortedAscending.sumOf { it.odometerValue }
        val totalRecordsCount = sortedAscending.count()

        var runningAccumulatedKms = 0.0

        // Precompute accumulated kilometers for each trip to determine if it is over the theoretical limit
        val recordsWithIndicators = sortedAscending.map { record ->
            runningAccumulatedKms += record.odometerValue
            val isOverLimit = isOverLimit(runningAccumulatedKms, record.timestamp, contract)
            RecordWithIndicator(record, isOverLimit)
        }

        return GetHistoryUseCase.Output.Success(
            contractId = contract.id,
            initialRecord = initialRecord,
            allRecords = recordsWithIndicators.sortedByDescending { it.record.timestamp },
            totalKms = totalKms,
            totalRecordsCount = totalRecordsCount
        )
    }

    private fun isOverLimit(currentTotalOdometer: Double, timestamp: Long, contract: RentingContract?): Boolean {
        if (contract == null) return false
        val totalDays = contract.durationMonths * DAYS_IN_MONTH
        val dailyLimit = contract.totalKms / totalDays

        val daysPassed = ((timestamp - contract.startDate) / MILLIS_IN_DAY.toDouble()).coerceAtLeast(0.0)

        val expectedKms = contract.startOdometer + (daysPassed * dailyLimit)

        return currentTotalOdometer > expectedKms
    }
}
