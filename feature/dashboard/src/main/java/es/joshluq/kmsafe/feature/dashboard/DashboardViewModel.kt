package es.joshluq.kmsafe.feature.dashboard

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.domain.model.AppOverlayState
import es.joshluq.kmsafe.domain.usecase.GetCurrentUserUseCase
import es.joshluq.kmsafe.domain.usecase.GetRentingContractUseCase
import es.joshluq.kmsafe.domain.usecase.ObserveAppOverlayUseCase
import es.joshluq.kmsafe.domain.usecase.ObserveFleetSwitchingUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import es.joshluq.kmsafe.core.analytics.AnalyticsTracker
import javax.inject.Inject

/**
 * ViewModel for managing the state of the DashboardScreen using the MVI pattern.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getRentingContractUseCase: GetRentingContractUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeFleetSwitchingUseCase: ObserveFleetSwitchingUseCase,
    private val observeAppOverlayUseCase: ObserveAppOverlayUseCase,
    private val analytics: AnalyticsTracker,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    private var lastUserId: String? = null

    override fun createInitialState(): State = State.Empty

    init {
        observeRentingContract()
        observeCurrentUser()
        observeFleetSwitching()
        observeAppOverlay()
    }

    override fun handleEvent(event: Event) {
        logger.d("DashboardViewModel", "Event received: $event")
        when (event) {
            is Event.OnTabSelected -> handleTabSelected(event.tab)
            is Event.OnTabSynced -> handleTabSynced(event.tab)
            Event.ResetToOverview -> {
                updateState { copy(selectedTab = DashboardTab.OVERVIEW) }
            }
            Event.OnOdometerClicked -> {
                updateState { copy(showUpdateDialog = true, currentMileageInput = "") }
            }
            Event.OnDismissOdometerDialog -> {
                updateState { copy(showUpdateDialog = false) }
            }
            is Event.OnOdometerChanged -> {
                updateState { copy(currentMileageInput = event.mileage) }
            }
        }
    }

    private fun observeCurrentUser() {
        getCurrentUserUseCase(GetCurrentUserUseCase.Input)
            .onEach { output ->
                when (output) {
                    is GetCurrentUserUseCase.Output.Success -> {
                        val currentUserId = output.user?.id
                        if (currentUserId != null && currentUserId != lastUserId) {
                            logger.d("DashboardViewModel", "User session changed ($lastUserId -> $currentUserId), resetting tab to OVERVIEW")
                            lastUserId = currentUserId
                            if (!state.value.isNavigationBlocked) {
                                updateState { copy(selectedTab = DashboardTab.OVERVIEW) }
                            }
                        } else if (currentUserId == null) {
                            lastUserId = null
                        }
                    }
                }
            }
            .catch { logger.e("DashboardViewModel", "Error observing current user", it) }
            .launchIn(viewModelScope)
    }

    private fun observeRentingContract() {
        getRentingContractUseCase(GetRentingContractUseCase.Input)
            .onEach { output ->
                when (output) {
                    is GetRentingContractUseCase.Output.Success -> {
                        updateState { copy(hasRentingContract = true) }
                    }
                    is GetRentingContractUseCase.Output.Failure -> {
                        updateState { copy(hasRentingContract = false) }
                        logger.w("DashboardViewModel", "No active renting contract found")
                    }
                    else -> Unit
                }
            }.launchIn(viewModelScope)
    }

    private fun observeFleetSwitching() {
        observeFleetSwitchingUseCase(ObserveFleetSwitchingUseCase.Input)
            .onEach { output ->
                when (output) {
                    is ObserveFleetSwitchingUseCase.Output.Success -> {
                        updateState {
                            copy(
                                isSwitchingVehicle = output.state.isSwitching,
                                switchingVehicleName = output.state.vehicleName
                            )
                        }
                    }
                }
            }
            .catch { logger.e("DashboardViewModel", "Error observing fleet switching", it) }
            .launchIn(viewModelScope)
    }

    private fun observeAppOverlay() {
        observeAppOverlayUseCase(ObserveAppOverlayUseCase.Input)
            .onEach { output ->
                when (output) {
                    is ObserveAppOverlayUseCase.Output.Success -> {
                        updateState {
                            copy(
                                hudOverlayState = output.state,
                                isSwitchingVehicle = output.state is AppOverlayState.VehicleSwitching,
                                switchingVehicleName = (output.state as? AppOverlayState.VehicleSwitching)?.vehicleName
                            )
                        }
                    }
                }
            }
            .catch { logger.e("DashboardViewModel", "Error observing app overlay", it) }
            .launchIn(viewModelScope)
    }

    private fun handleTabSelected(tab: DashboardTab) {
        if (state.value.isNavigationBlocked) {
            logger.d("DashboardViewModel", "Ignoring tab selection while global overlay is active: ${state.value.hudOverlayState}")
            return
        }
        if (state.value.selectedTab != tab) {
            analytics.trackScreen("dashboard_${tab.name.lowercase()}", "DashboardScreen")
            updateState { copy(selectedTab = tab) }
            logger.d("DashboardViewModel", "Navigating to tab $tab")
            launchEffect(Effect.NavigateToTab(tab))
        }
    }

    private fun handleTabSynced(tab: DashboardTab) {
        if (state.value.selectedTab != tab) {
            updateState { copy(selectedTab = tab) }
        }
    }
}
