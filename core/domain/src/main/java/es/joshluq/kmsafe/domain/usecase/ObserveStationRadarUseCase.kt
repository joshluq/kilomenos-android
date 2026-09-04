package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.StationRadarItem
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import es.joshluq.kmsafe.domain.repository.FuelExpenseRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import es.joshluq.kmsafe.domain.repository.ServiceStationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Domain interface to observe the "My Stations" price radar with real-time delta badges.
 */
interface ObserveStationRadarUseCase :
    FlowUseCase<ObserveStationRadarUseCase.Input, ObserveStationRadarUseCase.Output> {

    data class Input(val vehicleId: String? = null) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(
            val items: List<StationRadarItem>,
            val isLocked: Boolean
        ) : Output
    }
}

class ObserveStationRadarUseCaseImpl @Inject constructor(
    private val stationRepository: ServiceStationRepository,
    private val expenseRepository: FuelExpenseRepository,
    private val rentingRepository: RentingRepository,
    private val entitlementsRepository: EntitlementsRepository,
    private val logger: LoggerKit
) : ObserveStationRadarUseCase {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(input: ObserveStationRadarUseCase.Input): Flow<ObserveStationRadarUseCase.Output> {
        logger.d("ObserveStationRadarUseCase", "Observing price radar for vehicle: ${input.vehicleId ?: "Active"}")

        val vehicleFlow = if (input.vehicleId != null) {
            rentingRepository.getContractById(input.vehicleId)
        } else {
            rentingRepository.getContract()
        }

        return vehicleFlow.flatMapLatest { contract ->
            if (contract == null) {
                return@flatMapLatest flowOf(ObserveStationRadarUseCase.Output.Success(emptyList(), isLocked = false))
            }

            val stationsFlow = stationRepository.getAllStations()
            val expensesFlow = expenseRepository.getExpensesByVehicle(contract.id)
            val entitlementsFlow = entitlementsRepository.observeEntitlements()

            combine(stationsFlow, expensesFlow, entitlementsFlow) { stations, expenses, entitlements ->
                val isUnlocked = entitlements.isFeatureActive(Feature.STATION_PRICE_RADAR)
                if (!isUnlocked) {
                    logger.d("ObserveStationRadarUseCase", "Station price radar is locked for free tier user")
                    return@combine ObserveStationRadarUseCase.Output.Success(
                        items = emptyList(),
                        isLocked = true
                    )
                }

                if (stations.isEmpty() || expenses.isEmpty()) {
                    return@combine ObserveStationRadarUseCase.Output.Success(
                        items = emptyList(),
                        isLocked = false
                    )
                }

                val radarItems = stations.mapNotNull { station ->
                    val stationExpenses = expenses.filter {
                        (it.stationId == station.id || it.stationName.equals(station.name, ignoreCase = true)) &&
                            it.unitPrice > 0.0
                    }

                    if (stationExpenses.isEmpty()) return@mapNotNull null

                    val latestExpense = stationExpenses.maxByOrNull { it.timestamp } ?: return@mapNotNull null
                    val targetFuelType = latestExpense.fuelType
                    val sameTypeExpenses = stationExpenses.filter { it.fuelType == targetFuelType }

                    val lastPrice = latestExpense.unitPrice
                    val avgPrice = sameTypeExpenses.map { it.unitPrice }.average()
                    val delta = lastPrice - avgPrice
                    val isOpportunity = delta <= -0.02 // At least 2 cents/L or kWh cheaper than personal average

                    StationRadarItem(
                        station = station,
                        fuelType = targetFuelType,
                        lastRecordedPrice = lastPrice,
                        userAveragePrice = avgPrice,
                        priceDelta = delta,
                        isOpportunity = isOpportunity,
                        bestDayPrediction = null
                    )
                }

                logger.i("ObserveStationRadarUseCase", "Emitting ${radarItems.size} station radar items")
                ObserveStationRadarUseCase.Output.Success(
                    items = radarItems,
                    isLocked = false
                )
            }
        }.catch { e ->
            logger.e("ObserveStationRadarUseCase", "Error observing station radar", e)
            emit(ObserveStationRadarUseCase.Output.Success(items = emptyList(), isLocked = false))
        }
    }
}
