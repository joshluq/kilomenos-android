package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.TripProjection
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Domain interface for calculating the predictive projection of mileage at the end of the contract.
 * Follows the linearly weighted projection algorithm.
 */
interface GetTripProjectionUseCase : FlowUseCase<GetTripProjectionUseCase.Input, GetTripProjectionUseCase.Output> {

    object Input : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data object Progress : Output
        data class Failure(val message: String) : Output
        data class Success(val projection: TripProjection?) : Output
    }
}

class GetTripProjectionUseCaseImpl @Inject constructor(
    private val rentingRepository: RentingRepository,
    private val historyRepository: HistoryRepository,
    private val logger: LoggerKit
) : GetTripProjectionUseCase {

    companion object {
        private const val DAYS_IN_MONTH = 30.4375
        private const val MILLIS_IN_DAY = 1000L * 60 * 60 * 24
        private const val MIN_DAYS_FOR_PROJECTION = 7
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: GetTripProjectionUseCase.Input): Flow<GetTripProjectionUseCase.Output> {
        logger.d("GetTripProjectionUseCase", "Calculating projection")
        return rentingRepository.getContract().flatMapLatest { contract ->
            if (contract == null) {
                logger.w("GetTripProjectionUseCase", "No active contract")
                return@flatMapLatest flowOf(GetTripProjectionUseCase.Output.Success(null) as GetTripProjectionUseCase.Output)
            }

            historyRepository.getHistory(contract.id).map { records ->
                logger.i("GetTripProjectionUseCase", "Crunching numbers for ${contract.vehicleName}")
                val currentTime = System.currentTimeMillis()
                val totalContractDays = contract.durationMonths * DAYS_IN_MONTH
                val elapsedMillis = (currentTime - contract.startDate).coerceAtLeast(0L)
                val elapsedDays = elapsedMillis / MILLIS_IN_DAY.toDouble()

                // Calculate total real kms driven (excluding initial record)
                val realKmsDriven = records.filter { !it.isInitialRecord }
                    .sumOf { it.odometerValue }

                val hasEnoughData = elapsedDays >= MIN_DAYS_FOR_PROJECTION

                val dailyAverage = if (elapsedDays > 0) realKmsDriven / elapsedDays else 0.0

                val projectedFinalKms = contract.startOdometer + (dailyAverage * totalContractDays)
                val contractedLimitKms = contract.startOdometer + contract.totalKms
                val expectedBalance = contractedLimitKms - projectedFinalKms

                val projection = TripProjection(
                    contractId = contract.id,
                    projectedTotalKms = projectedFinalKms,
                    expectedFinalBalance = expectedBalance,
                    isOverLimit = expectedBalance < 0,
                    dailyAverage = dailyAverage,
                    hasEnoughData = hasEnoughData
                )

                GetTripProjectionUseCase.Output.Success(projection) as GetTripProjectionUseCase.Output
            }
        }
            .onStart { emit(GetTripProjectionUseCase.Output.Progress) }
            .catch { emit(GetTripProjectionUseCase.Output.Failure(it.message ?: "Unknown error")) }
    }
}
