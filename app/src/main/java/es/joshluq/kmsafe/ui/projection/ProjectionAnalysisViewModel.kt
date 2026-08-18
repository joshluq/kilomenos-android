package es.joshluq.kmsafe.ui.projection

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.domain.di.GetOverviewData
import es.joshluq.kmsafe.domain.di.GetRenting
import es.joshluq.kmsafe.domain.di.GetTripProjection
import es.joshluq.kmsafe.domain.model.RentingContract
import es.joshluq.kmsafe.domain.usecase.GetOverviewDataUseCase
import es.joshluq.kmsafe.domain.usecase.GetRentingContractUseCase
import es.joshluq.kmsafe.domain.usecase.GetTripProjectionUseCase
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import javax.inject.Inject

@HiltViewModel
class ProjectionAnalysisViewModel @Inject constructor(
    @param:GetTripProjection private val getTripProjectionUseCase:
    @JvmSuppressWildcards FlowUseCase<GetTripProjectionUseCase.Input, GetTripProjectionUseCase.Output>,
    @param:GetOverviewData private val getOverviewDataUseCase:
    @JvmSuppressWildcards FlowUseCase<GetOverviewDataUseCase.Input, GetOverviewDataUseCase.Output>,
    @param:GetRenting private val getRentingContractUseCase:
    @JvmSuppressWildcards FlowUseCase<GetRentingContractUseCase.Input, GetRentingContractUseCase.Output>,
    private val analytics: AnalyticskitManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    companion object {
        private const val MILLIS_IN_DAY = 1000L * 60 * 60 * 24
        private const val DAYS_IN_MONTH = 30.4375
    }

    override fun createInitialState(): State = State.Empty

    init {
        observeData()
    }

    override fun handleEvent(event: Event) {
        logger.d("ProjectionAnalysisViewModel", "Event received: $event")
        when (event) {
            is Event.OnSimulatedKmChanged -> {
                updateState { copy(simulatedDailyKm = event.newValue) }
                recalculateSimulation()
            }
            is Event.OnPenaltyPriceChanged -> {
                updateState { copy(penaltyPricePerKm = event.newValue) }
                recalculateSimulation()
            }
            is Event.OnPlannedTripChanged -> {
                updateState { copy(plannedTripKms = event.newValue) }
                recalculateSimulation()
            }
            Event.OnDismissError -> updateState { copy(error = null) }
        }
    }

    private fun observeData() {
        analytics.track(AnalyticsEvent.Custom("projection_viewed"))
        combine(
            getTripProjectionUseCase(GetTripProjectionUseCase.Input),
            getOverviewDataUseCase(GetOverviewDataUseCase.Input),
            getRentingContractUseCase(GetRentingContractUseCase.Input)
        ) { projectionOutput, overviewOutput, rentingOutput ->
            if (projectionOutput is GetTripProjectionUseCase.Output.Success &&
                overviewOutput is GetOverviewDataUseCase.Output.Success &&
                rentingOutput is GetRentingContractUseCase.Output.Success
            ) {
                val contract = rentingOutput.contract
                val projection = projectionOutput.projection
                val totalDays = (contract.durationMonths * DAYS_IN_MONTH).toLong()
                val elapsedDays = ((System.currentTimeMillis() - contract.startDate) / MILLIS_IN_DAY).coerceAtLeast(0)
                val remainingDays = (totalDays - elapsedDays).coerceAtLeast(0)

                updateState {
                    copy(
                        isLoading = false,
                        baselineProjection = projection,
                        currentRealDailyAverage = projection?.dailyAverage?.toFloat() ?: 0f,
                        simulatedDailyKm = projection?.dailyAverage?.toFloat() ?: 0f,
                        simulatedFinalBalance = projection?.expectedFinalBalance ?: 0,
                        daysRemaining = remainingDays,
                        totalContractKms = contract.totalKms,
                        estimatedPenalty = if (projection != null) {
                            val balanceWithTrip = projection.expectedFinalBalance - plannedTripKms
                            if (balanceWithTrip < 0) {
                                balanceWithTrip.toDouble() * -penaltyPricePerKm
                            } else {
                                0.0
                            }
                        } else {
                            0.0
                        },
                        recommendedDailyKm = if (remainingDays > 0) {
                            val availableKms = (contract.totalKms + contract.startOdometer - (overviewOutput.actualKmsDrivenSinceStart + contract.startOdometer)).coerceAtLeast(
                                0.0
                            )
                            (availableKms / remainingDays).toInt()
                        } else {
                            null
                        }
                    )
                }

                actualKmsDrivenSinceStart = overviewOutput.actualKmsDrivenSinceStart
                contractData = contract
            }
        }.launchIn(viewModelScope)
    }

    private var actualKmsDrivenSinceStart: Double = 0.0
    private var contractData: RentingContract? = null

    private fun recalculateSimulation() {
        val contract = contractData ?: return

        val simulatedAdditionalKms = state.value.simulatedDailyKm * state.value.daysRemaining
        val totalProjectedKms = actualKmsDrivenSinceStart + contract.startOdometer + simulatedAdditionalKms + state.value.plannedTripKms

        val contractedLimitKms = contract.startOdometer + contract.totalKms
        val simulatedBalance = contractedLimitKms - totalProjectedKms

        updateState {
            copy(
                simulatedFinalBalance = simulatedBalance.toInt(),
                estimatedPenalty = if (simulatedBalance < 0) simulatedBalance * -penaltyPricePerKm else 0.0
            )
        }
    }
}
