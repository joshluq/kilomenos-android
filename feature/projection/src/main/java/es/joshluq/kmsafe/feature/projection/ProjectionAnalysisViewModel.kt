package es.joshluq.kmsafe.feature.projection

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.kmsafe.core.analytics.AnalyticsTracker
import es.joshluq.kmsafe.core.analytics.model.KmsafeAnalyticsEvent
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.PlannedTrip
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.GetOverviewDataUseCase
import es.joshluq.kmsafe.domain.usecase.GetRentingContractUseCase
import es.joshluq.kmsafe.domain.usecase.GetTripProjectionUseCase
import es.joshluq.kmsafe.domain.usecase.SimulateContractProjectionUseCase
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectionAnalysisViewModel @Inject constructor(
    private val getTripProjectionUseCase: GetTripProjectionUseCase,
    private val getOverviewDataUseCase: GetOverviewDataUseCase,
    private val getRentingContractUseCase: GetRentingContractUseCase,
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
    private val simulateContractProjectionUseCase: SimulateContractProjectionUseCase,
    private val analytics: AnalyticsTracker,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    companion object {
        private const val MILLIS_IN_DAY = 1000L * 60 * 60 * 24
        private const val DAYS_IN_MONTH = 30.4375
    }

    override fun createInitialState(): State = State.Empty

    private var currentContract: RentingContract? = null
    private var actualKmsDrivenSinceStart: Double = 0.0

    init {
        observeData()
    }

    override fun handleEvent(event: Event) {
        logger.d("ProjectionAnalysisViewModel", "Event received: $event")
        when (event) {
            is Event.OnSimulatedKmChanged -> handleSimulatedKmChanged(event.newValue)
            is Event.OnPacePresetSelected -> handlePacePresetSelected(event.multiplier)
            is Event.OnAddPresetTrip -> handleAddPresetTrip(event.title, event.distanceKms)
            is Event.OnRemoveTrip -> handleRemoveTrip(event.tripId)
            is Event.OnCustomTripChanged -> handleCustomTripChanged(event.distanceKms)
            is Event.OnPenaltyPriceChanged -> handlePenaltyPriceChanged(event.newPrice)
            Event.OnResetSimulation -> handleResetSimulation()
            Event.OnConfigureContractClicked -> {
                val vehicleId = state.value.activeVehicleId ?: currentContract?.id
                if (!vehicleId.isNullOrEmpty()) {
                    analytics.track(
                        KmsafeAnalyticsEvent.Projection.ConfigureContractClicked(
                            vehicleId = vehicleId,
                            source = "projection_analysis"
                        )
                    )
                    launchEffect(Effect.NavigateToEditContract(vehicleId))
                }
            }
            Event.OnUpgradeToPremiumClicked -> {
                analytics.track(KmsafeAnalyticsEvent.Monetization.UpgradeClicked(source = "projection_risk_sentinel"))
                launchEffect(Effect.NavigateToPremiumPaywall(source = "projection_risk_sentinel"))
            }
            Event.OnDismissError -> updateState { copy(error = null) }
        }
    }

    private fun observeData() {
        analytics.track(KmsafeAnalyticsEvent.Projection.ProjectionViewed)
        combine(
            getTripProjectionUseCase(GetTripProjectionUseCase.Input),
            getOverviewDataUseCase(GetOverviewDataUseCase.Input),
            getRentingContractUseCase(GetRentingContractUseCase.Input),
            checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.ADVANCED_PROJECTIONS))
        ) { projectionOutput, overviewOutput, rentingOutput, accessOutput ->
            if (projectionOutput is GetTripProjectionUseCase.Output.Success &&
                overviewOutput is GetOverviewDataUseCase.Output.Success &&
                rentingOutput is GetRentingContractUseCase.Output.Success
            ) {
                val contract = rentingOutput.contract
                val projection = projectionOutput.projection
                val isPremium = (accessOutput is CheckFeatureAccessUseCase.Output.Success) && accessOutput.isGranted

                val overviewContract = overviewOutput.contract
                // Guard against intermediate race conditions during vehicle switch: ensure contract IDs match
                if (overviewContract != null && overviewContract.id != contract.id) {
                    return@combine
                }
                if (projection != null && projection.contractId.isNotEmpty() && projection.contractId != contract.id) {
                    return@combine
                }

                val totalDays = (contract.durationMonths * DAYS_IN_MONTH).toLong()
                val elapsedMillis = (System.currentTimeMillis() - contract.startDate).coerceAtLeast(0L)
                val elapsedDays = (elapsedMillis / MILLIS_IN_DAY.toDouble()).coerceAtLeast(0.0)
                val remainingDays = (totalDays - elapsedDays.toLong()).coerceAtLeast(0L)
                val contractEndDate = contract.startDate + (totalDays * MILLIS_IN_DAY)

                val realDailyAvg = projection?.dailyAverage?.toFloat() ?: 0f
                val defaultPenaltyPrice = contract.excessDistancePrice?.toFloat() ?: RentingContract.DEFAULT_MARKET_EXCESS_PRICE

                val isVehicleChanged = currentContract != null && currentContract?.id != contract.id
                currentContract = contract
                actualKmsDrivenSinceStart = overviewOutput.actualKmsDrivenSinceStart

                // Reset simulation variables when vehicle changes or on initial load
                val initialSimDailyKm = if (isVehicleChanged || state.value.simulatedDailyKm == 0f) {
                    realDailyAvg
                } else {
                    state.value.simulatedDailyKm
                }

                updateState {
                    copy(
                        isLoading = false,
                        isPremium = isPremium,
                        activeVehicleId = contract.id,
                        totalContractKms = contract.totalKms,
                        startOdometer = contract.startOdometer,
                        contractEndDateMillis = contractEndDate,
                        daysRemaining = remainingDays,
                        penaltyPricePerKm = defaultPenaltyPrice,
                        baselineProjection = projection,
                        realDailyAverage = realDailyAvg,
                        simulatedDailyKm = initialSimDailyKm,
                        paceMultiplier = if (isVehicleChanged) 1.0f else paceMultiplier,
                        plannedTrips = if (isVehicleChanged) emptyList() else plannedTrips,
                        totalPlannedTripsKm = if (isVehicleChanged) 0 else totalPlannedTripsKm
                    )
                }

                executeSimulation()
            }
        }.launchIn(viewModelScope)
    }

    private fun handleSimulatedKmChanged(newValue: Float) {
        val basePace = state.value.realDailyAverage
        val multiplier = if (basePace > 0f) newValue / basePace else 1.0f
        updateState {
            copy(
                simulatedDailyKm = newValue,
                paceMultiplier = multiplier
            )
        }
        executeSimulation()
    }

    private fun handlePacePresetSelected(multiplier: Float) {
        val basePace = state.value.realDailyAverage
        val newDailyKm = basePace * multiplier
        updateState {
            copy(
                simulatedDailyKm = newDailyKm,
                paceMultiplier = multiplier
            )
        }
        executeSimulation()
    }

    private fun handleAddPresetTrip(title: String, distanceKms: Int) {
        // Enforce Freemium rule: Free users can only have 1 active planned trip
        if (!state.value.isPremium && state.value.plannedTrips.isNotEmpty()) {
            analytics.track(KmsafeAnalyticsEvent.Projection.MultiTripBlockedFree)
            launchEffect(Effect.NavigateToPremiumPaywall(source = "projection_multi_trip"))
            return
        }

        val newTrip = PlannedTrip(title = title, distanceKms = distanceKms)
        val updatedTrips = state.value.plannedTrips + newTrip
        updateState {
            copy(
                plannedTrips = updatedTrips,
                totalPlannedTripsKm = updatedTrips.sumOf { it.distanceKms }
            )
        }
        executeSimulation()
    }

    private fun handleRemoveTrip(tripId: String) {
        val updatedTrips = state.value.plannedTrips.filter { it.id != tripId }
        updateState {
            copy(
                plannedTrips = updatedTrips,
                totalPlannedTripsKm = updatedTrips.sumOf { it.distanceKms }
            )
        }
        executeSimulation()
    }

    private fun handleCustomTripChanged(distanceKms: Int) {
        val updatedTrips = if (distanceKms <= 0) {
            emptyList()
        } else {
            listOf(PlannedTrip(title = "Escapada Personalizada", distanceKms = distanceKms))
        }
        updateState {
            copy(
                plannedTrips = updatedTrips,
                totalPlannedTripsKm = updatedTrips.sumOf { it.distanceKms }
            )
        }
        executeSimulation()
    }

    private fun handlePenaltyPriceChanged(newPrice: Float) {
        updateState { copy(penaltyPricePerKm = newPrice) }
        executeSimulation()
    }

    private fun handleResetSimulation() {
        val realAvg = state.value.realDailyAverage
        updateState {
            copy(
                simulatedDailyKm = realAvg,
                paceMultiplier = 1.0f,
                plannedTrips = emptyList(),
                totalPlannedTripsKm = 0
            )
        }
        executeSimulation()
    }

    private fun executeSimulation() {
        val contract = currentContract ?: return

        viewModelScope.launch {
            val input = SimulateContractProjectionUseCase.Input(
                contract = contract,
                actualKmsDrivenSinceStart = actualKmsDrivenSinceStart,
                simulatedDailyKm = state.value.simulatedDailyKm,
                plannedTrips = state.value.plannedTrips,
                penaltyPricePerKm = state.value.penaltyPricePerKm
            )

            val result = simulateContractProjectionUseCase(input).getOrNull()
            if (result is SimulateContractProjectionUseCase.Output.Success) {
                val sim = result.result
                updateState {
                    copy(
                        simulatedProjectedTotalKms = sim.simulatedProjectedTotalKms,
                        simulatedFinalBalance = sim.simulatedFinalBalance,
                        estimatedPenalty = sim.estimatedPenalty,
                        exhaustionDateMillis = sim.exhaustionDateMillis,
                        monthsAheadOrBehind = sim.monthsAheadOrBehind,
                        remedialDailyKm = sim.remedialDailyKm,
                        grossExcessKms = sim.grossExcessKms,
                        courtesyMarginKms = sim.courtesyMarginKms,
                        billableExcessKms = sim.billableExcessKms,
                        ratePerKm = sim.ratePerKm,
                        courtesySavingsAmount = sim.courtesySavingsAmount,
                        isUsingDefaultPrice = sim.isUsingDefaultPrice,
                        isUsingDefaultCourtesyMargin = sim.isUsingDefaultCourtesyMargin
                    )
                }

                analytics.track(
                    KmsafeAnalyticsEvent.Projection.FinancialImpactViewed(
                        isOverLimit = sim.isOverLimit,
                        estimatedPenalty = sim.estimatedPenalty,
                        billableExcessKms = sim.billableExcessKms
                    )
                )
            }
        }
    }
}
