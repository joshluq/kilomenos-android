package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.ContractMetrics
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.model.SyncStatus
import javax.inject.Inject

/**
 * Pure domain use case that executes the KiloMenos algorithm to calculate
 * the metrics and balance of a renting contract.
 */
class CalculateContractMetricsUseCase @Inject constructor() :
    UseCase<CalculateContractMetricsUseCase.Input, CalculateContractMetricsUseCase.Output> {

    companion object {
        const val DAYS_IN_MONTH = 30.4375
        const val MILLIS_IN_DAY = 1000L * 60 * 60 * 24
    }

    override suspend fun invoke(input: Input): Result<Output> {
        val metrics = calculate(
            contract = input.contract,
            records = input.records,
            currentTime = input.currentTime
        )
        return Result.success(Output.Success(metrics))
    }

    /**
     * Synchronously calculates [ContractMetrics] using the KiloMenos algorithm.
     */
    fun calculate(
        contract: RentingContract,
        records: List<OdometerRecord>,
        currentTime: Long = System.currentTimeMillis()
    ): ContractMetrics {
        val totalKmsDriven = records.filter { !it.isInitialRecord }
            .sumOf { it.odometerValue }
            .coerceAtLeast(0.0)

        val pendingRecords = records.filter { it.syncStatus == SyncStatus.PENDING }
        val hasPendingRecords = pendingRecords.isNotEmpty()
        val isSyncPending = hasPendingRecords || contract.syncStatus == SyncStatus.PENDING

        val totalDays = contract.durationMonths * DAYS_IN_MONTH
        val daysPassed = ((currentTime - contract.startDate) / MILLIS_IN_DAY.toDouble()).coerceAtLeast(0.0)
        val baseDailyBudget = if (totalDays > 0) contract.totalKms / totalDays else 0.0
        val monthlyBudget = if (contract.durationMonths > 0) contract.totalKms / contract.durationMonths else 0.0
        val theoreticalKms = daysPassed * baseDailyBudget
        val balance = theoreticalKms - totalKmsDriven

        val timeUsedPercentage = (if (totalDays > 0) daysPassed / totalDays else 0.0).coerceIn(0.0, 1.0).toFloat()
        val kmsUsedPercentage = (if (contract.totalKms > 0) totalKmsDriven / contract.totalKms else 0.0).coerceIn(0.0, 1.0).toFloat()
        val differencePercentage = ((timeUsedPercentage - kmsUsedPercentage) * 100)
        val currentOdometer = contract.startOdometer + totalKmsDriven

        return ContractMetrics(
            contract = contract,
            actualKmsDriven = totalKmsDriven,
            currentOdometer = currentOdometer,
            theoreticalKms = theoreticalKms,
            balance = balance,
            dailyBudget = baseDailyBudget,
            monthlyBudget = monthlyBudget,
            timePercentage = timeUsedPercentage,
            kmsPercentage = kmsUsedPercentage,
            differencePercentage = differencePercentage,
            isSyncPending = isSyncPending
        )
    }

    data class Input(
        val contract: RentingContract,
        val records: List<OdometerRecord>,
        val currentTime: Long = System.currentTimeMillis()
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val metrics: ContractMetrics) : Output
    }
}
