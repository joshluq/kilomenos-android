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
 * Use case to retrieve the odometer history grouped by month.
 */
class GetHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val rentingRepository: RentingRepository,
    private val logger: LoggerKit
) : FlowUseCase<GetHistoryUseCase.Input, GetHistoryUseCase.Output> {

    companion object {
        private const val DAYS_IN_MONTH = 30.4375
        private const val MILLIS_IN_DAY = 1000L * 60 * 60 * 24
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: Input): Flow<Output> {
        logger.d("GetHistoryUseCase", "Fetching history (forceRefresh=${input.forceRefresh})")
        return rentingRepository.getContract().flatMapLatest { contract ->
            if (contract == null) {
                logger.w("GetHistoryUseCase", "No active contract")
                return@flatMapLatest flowOf(Output.Empty)
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
        }
            .onStart { emit(Output.Progress) }
            .catch { emit(Output.Failure) }
    }

    private fun processRecords(records: List<OdometerRecord>, contract: RentingContract): Output.Success {
        logger.i("GetHistoryUseCase", "Processing ${records.size} records for contract ${contract.id}")
        val initialRecord = records.find { it.isInitialRecord }
        val activityRecords = records.filter { !it.isInitialRecord }
            .sortedBy { it.timestamp }

        val totalKms = activityRecords.sumOf { it.odometerValue }
        val totalRecordsCount = activityRecords.size

        var runningAccumulatedKms = initialRecord?.odometerValue ?: contract.startOdometer

        val recordsWithIndicators = activityRecords.map { record ->
            runningAccumulatedKms += record.odometerValue
            val isOverLimit = isOverLimit(runningAccumulatedKms, record.timestamp, contract)
            RecordWithIndicator(record, isOverLimit)
        }

        return Output.Success(
            contractId = contract.id,
            initialRecord = initialRecord,
            allRecords = recordsWithIndicators.sortedByDescending { it.record.timestamp },
            totalKms = totalKms,
            totalRecordsCount = totalRecordsCount
        )
    }

    private fun isOverLimit(currentTotalOdometer: Int, timestamp: Long, contract: RentingContract?): Boolean {
        if (contract == null) return false
        val totalDays = contract.durationMonths * DAYS_IN_MONTH
        val dailyLimit = contract.totalKms / totalDays

        val daysPassed = ((timestamp - contract.startDate) / MILLIS_IN_DAY.toDouble()).coerceAtLeast(0.0)

        val expectedKms = contract.startOdometer + (daysPassed * dailyLimit)

        return currentTotalOdometer > expectedKms
    }

    data class Input(val forceRefresh: Boolean = false) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data object Failure : Output
        data object Empty : Output
        data class Success(
            val contractId: String,
            val initialRecord: OdometerRecord?,
            val allRecords: List<RecordWithIndicator>,
            val totalKms: Int,
            val totalRecordsCount: Int
        ) : Output
    }
}
