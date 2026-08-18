package es.joshluq.kmsafe.ui.dashboard

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.domain.di.GetRenting
import es.joshluq.kmsafe.domain.usecase.GetRentingContractUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * ViewModel for managing the state of the DashboardScreen using the MVI pattern.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    @param:GetRenting private val getRentingContractUseCase:
    @JvmSuppressWildcards FlowUseCase<GetRentingContractUseCase.Input, GetRentingContractUseCase.Output>,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    init {
        observeRentingContract()
    }

    override fun createInitialState(): State = State.Empty

    override fun handleEvent(event: Event) {
        logger.d("DashboardViewModel", "Event received: $event")
        when (event) {
            is Event.OnTabSelected -> handleTabSelection(event.tab)
            is Event.OnTabSynced -> handleTabSync(event.tab)
            Event.OnUpdateOdometerClicked -> handleUpdateOdometerClicked()
            Event.OnDismissOdometerDialog -> handleDismissOdometerDialog()
            is Event.OnMileageInputChanged -> handleMileageInputChanged(event.mileage)
        }
    }

    private fun observeRentingContract() {
        getRentingContractUseCase(GetRentingContractUseCase.Input)
            .onEach { output ->
                if (output is GetRentingContractUseCase.Output.Success) {
                    updateState { copy(hasRentingContract = true) }
                } else if (output is GetRentingContractUseCase.Output.Failure) {
                    updateState { copy(hasRentingContract = false) }
                    // Auto-redirect if we are on Projection tab but contract is gone
                    if (state.value.selectedTab == DashboardTab.PROJECTION) {
                        handleTabSelection(DashboardTab.OVERVIEW)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleTabSelection(tab: DashboardTab) {
        if (state.value.selectedTab != tab) {
            updateState { copy(selectedTab = tab) }
            logger.d("DashboardViewModel", "Effect launched: NavigateToTab $tab")
            launchEffect(Effect.NavigateToTab(tab))
        }
    }

    private fun handleTabSync(tab: DashboardTab) {
        if (state.value.selectedTab != tab) {
            updateState { copy(selectedTab = tab) }
        }
    }

    private fun handleUpdateOdometerClicked() {
        updateState { copy(showUpdateDialog = true, currentMileageInput = "") }
    }

    private fun handleDismissOdometerDialog() {
        updateState { copy(showUpdateDialog = false) }
    }

    private fun handleMileageInputChanged(mileage: String) {
        updateState { copy(currentMileageInput = mileage) }
    }
}
