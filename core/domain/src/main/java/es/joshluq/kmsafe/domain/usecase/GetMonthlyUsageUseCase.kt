package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.MonthlyOdometerAggregation
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.util.Calendar
import javax.inject.Inject

/**
 * Domain interface to retrieve monthly kilometer usage aggregations.
 */
interface GetMonthlyUsageUseCase : FlowUseCase<GetMonthlyUsageUseCase.Input, GetMonthlyUsageUseCase.Output> {

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        object Progress : Output
        object Failure : Output
        data class Success(val aggregations: List<MonthlyOdometerAggregation>) : Output
    }
}

class GetMonthlyUsageUseCaseImpl @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val rentingRepository: RentingRepository,
    private val logger: LoggerKit
) : GetMonthlyUsageUseCase {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: GetMonthlyUsageUseCase.Input): Flow<GetMonthlyUsageUseCase.Output> {
        logger.d("GetMonthlyUsageUseCase", "Calculating monthly usage")
        return rentingRepository.getContract().flatMapLatest { contract ->
            if (contract == null) {
                logger.w("GetMonthlyUsageUseCase", "No active contract")
                return@flatMapLatest flowOf(GetMonthlyUsageUseCase.Output.Success(emptyList()) as GetMonthlyUsageUseCase.Output)
            }

            historyRepository.getHistory(contract.id).map { records ->
                logger.i("GetMonthlyUsageUseCase", "Processing ${records.size} records for aggregation")
                if (records.isEmpty()) {
                    return@map GetMonthlyUsageUseCase.Output.Success(emptyList()) as GetMonthlyUsageUseCase.Output
                }

                val monthlyLimit = contract.totalKms / contract.durationMonths
                val calendar = Calendar.getInstance()

                // Filter out the initial record and focus on activity records (increments)
                val activityRecords = records.filter { !it.isInitialRecord }

                // Group by Year-Month pairs
                val groupedByMonth = activityRecords.groupBy { record ->
                    calendar.timeInMillis = record.timestamp
                    Pair(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
                }

                val aggregations = groupedByMonth.map { (yearMonth, monthRecords) ->
                    val (year, month) = yearMonth

                    // Sum all odometer values for the month (each record is an increment)
                    val kmsDriven = monthRecords.sumOf { it.odometerValue }

                    MonthlyOdometerAggregation(
                        year = year,
                        month = month,
                        totalKms = kmsDriven,
                        budgetedKms = monthlyLimit
                    )
                }.sortedWith(compareBy({ it.year }, { it.month }))

                GetMonthlyUsageUseCase.Output.Success(aggregations) as GetMonthlyUsageUseCase.Output
            }
        }
            .onStart { emit(GetMonthlyUsageUseCase.Output.Progress) }
            .catch { emit(GetMonthlyUsageUseCase.Output.Failure) }
    }
}
