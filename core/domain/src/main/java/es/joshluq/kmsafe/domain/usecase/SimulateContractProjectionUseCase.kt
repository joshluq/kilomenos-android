package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.PlannedTrip
import es.joshluq.kmsafe.domain.model.ProjectionSimulationResult
import es.joshluq.kmsafe.domain.model.RentingContract
import javax.inject.Inject
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Pure domain use case that executes multi-variable contract pacing and trip simulation.
 * Calculates expected final balance, penalty, exhaustion date, and the remedial pace needed for 0 € penalty.
 */
interface SimulateContractProjectionUseCase :
    UseCase<SimulateContractProjectionUseCase.Input, SimulateContractProjectionUseCase.Output> {

    companion object {
        const val DAYS_IN_MONTH = 30.4375
        const val MILLIS_IN_DAY = 1000L * 60 * 60 * 24
    }

    data class Input(
        val contract: RentingContract,
        val actualKmsDrivenSinceStart: Double,
        val simulatedDailyKm: Float,
        val plannedTrips: List<PlannedTrip> = emptyList(),
        val penaltyPricePerKm: Float? = null,
        val currentTime: Long = System.currentTimeMillis()
    ) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(val result: ProjectionSimulationResult) : Output
    }
}

class SimulateContractProjectionUseCaseImpl @Inject constructor() : SimulateContractProjectionUseCase {

    override suspend fun invoke(input: SimulateContractProjectionUseCase.Input): Result<SimulateContractProjectionUseCase.Output> {
        val contract = input.contract
        val currentTime = input.currentTime

        val totalContractDays = contract.durationMonths * SimulateContractProjectionUseCase.DAYS_IN_MONTH
        val elapsedMillis = (currentTime - contract.startDate).coerceAtLeast(0L)
        val elapsedDays = (elapsedMillis / SimulateContractProjectionUseCase.MILLIS_IN_DAY.toDouble()).coerceAtLeast(0.0)
        val remainingDays = (totalContractDays - elapsedDays).coerceAtLeast(0.0)
        val contractEndDateMillis = contract.startDate + (totalContractDays * SimulateContractProjectionUseCase.MILLIS_IN_DAY).toLong()

        val totalPlannedTripsKm = input.plannedTrips.sumOf { it.distanceKms }
        val simulatedAdditionalKms = (input.simulatedDailyKm * remainingDays) + totalPlannedTripsKm
        val totalProjectedKms = input.actualKmsDrivenSinceStart + contract.startOdometer + simulatedAdditionalKms

        val contractedLimitKms = contract.startOdometer + contract.totalKms
        val simulatedBalance = contractedLimitKms - totalProjectedKms
        val isOverLimit = simulatedBalance < 0.0

        // Financial Settlement Resolution: Check if contract has configured price/margin, else apply market defaults
        val isUsingDefaultPrice = contract.excessDistancePrice == null || contract.excessDistancePrice <= 0.0
        val effectivePricePerKm = input.penaltyPricePerKm
            ?: contract.excessDistancePrice?.toFloat()
            ?: RentingContract.DEFAULT_MARKET_EXCESS_PRICE

        val isUsingDefaultCourtesyMargin = contract.courtesyMarginKms <= 0.0 && contract.excessDistancePrice == null
        val effectiveCourtesyMargin = if (contract.courtesyMarginKms > 0.0) {
            contract.courtesyMarginKms
        } else if (contract.excessDistancePrice == null) {
            RentingContract.DEFAULT_MARKET_COURTESY_MARGIN_KMS
        } else {
            0.0
        }

        val grossExcessKms = if (isOverLimit) simulatedBalance.absoluteValue else 0.0
        val billableExcessKms = (grossExcessKms - effectiveCourtesyMargin).coerceAtLeast(0.0)
        val estimatedPenalty = billableExcessKms * effectivePricePerKm
        val courtesySavingsAmount = kotlin.math.min(grossExcessKms, effectiveCourtesyMargin) * effectivePricePerKm

        // Exhaustion Date Calculation
        val availableRemainingKms = (contract.totalKms - input.actualKmsDrivenSinceStart - totalPlannedTripsKm).coerceAtLeast(0.0)
        val (exhaustionDateMillis, monthsAheadOrBehind) = if (isOverLimit && input.simulatedDailyKm > 0f) {
            val daysUntilExhaustion = availableRemainingKms / input.simulatedDailyKm
            val exhaustionTime = currentTime + (daysUntilExhaustion * SimulateContractProjectionUseCase.MILLIS_IN_DAY).toLong()
            val diffMillis = contractEndDateMillis - exhaustionTime
            val diffMonths = ((diffMillis / (SimulateContractProjectionUseCase.DAYS_IN_MONTH * SimulateContractProjectionUseCase.MILLIS_IN_DAY)).roundToInt()).coerceAtLeast(0)
            Pair(exhaustionTime, diffMonths)
        } else {
            Pair(null, 0)
        }

        // Remedial Daily Pace Calculation (Target for 0 € penalty)
        val remainingAllowedKms = contract.totalKms - input.actualKmsDrivenSinceStart - totalPlannedTripsKm
        val remedialDailyKm = if (remainingDays > 0.0) {
            if (remainingAllowedKms <= 0.0) {
                0.0
            } else {
                remainingAllowedKms / remainingDays
            }
        } else {
            null
        }

        val result = ProjectionSimulationResult(
            simulatedProjectedTotalKms = totalProjectedKms,
            simulatedFinalBalance = simulatedBalance,
            estimatedPenalty = estimatedPenalty,
            exhaustionDateMillis = exhaustionDateMillis,
            monthsAheadOrBehind = monthsAheadOrBehind,
            remedialDailyKm = remedialDailyKm,
            isOverLimit = isOverLimit,
            grossExcessKms = grossExcessKms,
            courtesyMarginKms = effectiveCourtesyMargin,
            billableExcessKms = billableExcessKms,
            ratePerKm = effectivePricePerKm,
            courtesySavingsAmount = courtesySavingsAmount,
            isUsingDefaultPrice = isUsingDefaultPrice,
            isUsingDefaultCourtesyMargin = isUsingDefaultCourtesyMargin
        )

        return Result.success(SimulateContractProjectionUseCase.Output.Success(result))
    }
}
