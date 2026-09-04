package es.joshluq.kmsafe.feature.fleet.vehicles

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteContractUseCase
import es.joshluq.kmsafe.domain.usecase.GetAllContractsUseCase
import es.joshluq.kmsafe.domain.usecase.SelectContractUseCase
import es.joshluq.kmsafe.feature.fleet.R
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import es.joshluq.kmsafe.core.ui.R as CoreR

@HiltViewModel
class VehicleListViewModel @Inject constructor(
    private val getAllContractsUseCase: GetAllContractsUseCase,
    private val deleteContractUseCase: DeleteContractUseCase,
    private val selectContractUseCase: SelectContractUseCase,
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
    private val analytics: AnalyticskitManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    init {
        checkPremiumStatus()
        loadVehicles()
    }

    override fun createInitialState(): State = State()

    override fun handleEvent(event: Event) {
        logger.d("VehicleListViewModel", "Event received: $event")
        when (event) {
            is Event.OnDeleteVehicleClicked -> {
                if (event.vehicle.isSelected) {
                    updateState { copy(error = TextProvider.Resource(R.string.onboarding_error_delete_selected)) }
                } else {
                    updateState { copy(vehicleToDelete = event.vehicle) }
                }
            }
            Event.OnDeleteConfirmed -> handleDelete()
            Event.OnDeleteCancelled -> updateState {
                copy(vehicleToDelete = null)
            }
            is Event.OnVehicleSelected -> handleSwitch(event.id)
            Event.OnAddVehicleClicked -> handleAddVehicle()
            Event.OnDismissError -> updateState { copy(error = null) }
            Event.OnBackClicked -> launchEffect(Effect.NavigateBack)
            is Event.OnVehicleDetailsClicked -> launchEffect(Effect.NavigateToVehicleDetails(event.id))
            Event.OnUpgradeClicked -> launchEffect(Effect.NavigateToPremiumPaywall)
            Event.OnDismissPremiumLimit -> updateState { copy(showPremiumLimit = false) }
        }
    }

    private fun checkPremiumStatus() {
        checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.MULTI_VEHICLE))
            .onEach { output ->
                if (output is CheckFeatureAccessUseCase.Output.Success) {
                    updateState { copy(isPremium = output.isGranted) }
                }
            }.launchIn(viewModelScope)
    }

    private fun loadVehicles() {
        getAllContractsUseCase(GetAllContractsUseCase.Input)
            .onEach { output ->
                when (output) {
                    is GetAllContractsUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                    is GetAllContractsUseCase.Output.Success -> updateState {
                        copy(
                            isLoading = false,
                            vehicles = output.contracts
                        )
                    }
                    is GetAllContractsUseCase.Output.Failure -> updateState {
                        copy(
                            isLoading = false,
                            error = TextProvider.Resource(CoreR.string.history_load_error)
                        )
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun handleSwitch(vehicleId: String) {
        selectContractUseCase(SelectContractUseCase.Input(vehicleId))
            .onEach { output ->
                if (output is SelectContractUseCase.Output.Success) {
                    analytics.track(AnalyticsEvent.Custom("vehicle_switched"))
                    loadVehicles()
                }
            }.launchIn(viewModelScope)
    }

    private fun handleAddVehicle() {
        if (!state.value.isPremium && state.value.vehicles.isNotEmpty()) {
            analytics.track(AnalyticsEvent.Custom("multi_vehicle_limit_reached"))
            updateState { copy(showPremiumLimit = true) }
        } else {
            launchEffect(Effect.NavigateToAddVehicle)
        }
    }

    private fun handleDelete() {
        val vehicle = state.value.vehicleToDelete ?: return

        deleteContractUseCase(DeleteContractUseCase.Input(vehicle.id))
            .onEach { output ->
                when (output) {
                    is DeleteContractUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                    is DeleteContractUseCase.Output.Success -> {
                        analytics.track(AnalyticsEvent.Custom("vehicle_deleted"))
                        updateState { copy(isLoading = false, vehicleToDelete = null) }
                        loadVehicles()
                    }
                    is DeleteContractUseCase.Output.Failure -> {
                        updateState {
                            copy(
                                isLoading = false,
                                vehicleToDelete = null,
                                error = TextProvider.Resource(CoreR.string.history_register_error)
                            )
                        }
                    }
                }
            }.launchIn(viewModelScope)
    }
}
