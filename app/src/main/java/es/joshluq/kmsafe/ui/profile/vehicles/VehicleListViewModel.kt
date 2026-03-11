package es.joshluq.kmsafe.ui.profile.vehicles

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.di.DeleteContract
import es.joshluq.kmsafe.di.GetAllContracts
import es.joshluq.kmsafe.di.GetCurrentUser
import es.joshluq.kmsafe.di.SelectContract
import es.joshluq.kmsafe.di.SyncContracts
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteContractUseCase
import es.joshluq.kmsafe.domain.usecase.GetAllContractsUseCase
import es.joshluq.kmsafe.domain.usecase.GetCurrentUserUseCase
import es.joshluq.kmsafe.domain.usecase.SelectContractUseCase
import es.joshluq.kmsafe.domain.usecase.SyncContractsUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class VehicleListViewModel @Inject constructor(
    @param:GetAllContracts private val getAllContractsUseCase:
    @JvmSuppressWildcards FlowUseCase<GetAllContractsUseCase.Input, GetAllContractsUseCase.Output>,
    @param:SelectContract private val selectContractUseCase:
    @JvmSuppressWildcards FlowUseCase<SelectContractUseCase.Input, SelectContractUseCase.Output>,
    @param:DeleteContract private val deleteContractUseCase:
    @JvmSuppressWildcards FlowUseCase<DeleteContractUseCase.Input, DeleteContractUseCase.Output>,
    @param:SyncContracts private val syncContractsUseCase:
    @JvmSuppressWildcards FlowUseCase<SyncContractsUseCase.Input, SyncContractsUseCase.Output>,
    @param:GetCurrentUser private val getCurrentUserUseCase:
    @JvmSuppressWildcards FlowUseCase<GetCurrentUserUseCase.Input, GetCurrentUserUseCase.Output>,
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
    private val analytics: AnalyticskitManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    init {
        syncVehicles()
        loadVehicles()
    }

    override fun createInitialState(): State = State()

    override fun handleEvent(event: Event) {
        logger.d("VehicleListViewModel", "Event received: $event")
        when (event) {
            is Event.OnVehicleSelected -> selectVehicle(event.id)
            is Event.OnVehicleDetailsClicked -> {
                logger.d("VehicleListViewModel", "Effect launched: NavigateToVehicleDetails")
                launchEffect(Effect.NavigateToVehicleDetails(event.id))
            }
            is Event.OnDeleteVehicleClicked -> updateState { copy(vehicleToDelete = event.vehicle) }
            Event.OnDeleteConfirmed -> deleteVehicle()
            Event.OnDeleteCancelled -> updateState { copy(vehicleToDelete = null) }
            Event.OnAddVehicleClicked -> handleAddVehicle()
            Event.OnBackClicked -> {
                logger.d("VehicleListViewModel", "Effect launched: NavigateBack")
                launchEffect(Effect.NavigateBack)
            }
            Event.OnDismissPremiumLimit -> updateState { copy(showPremiumLimit = false) }
            Event.OnDismissError -> updateState { copy(error = null) }
        }
    }

    private fun handleAddVehicle() {
        getCurrentUserUseCase(GetCurrentUserUseCase.Input)
            .onEach { output ->
                if (output is GetCurrentUserUseCase.Output.Success) {
                    val hasMultiVehicleAccess = checkFeatureAccessUseCase(
                        output.user,
                        CheckFeatureAccessUseCase.Feature.MULTI_VEHICLE
                    )
                    if (state.value.vehicles.isNotEmpty() && !hasMultiVehicleAccess) {
                        analytics.track(
                            AnalyticsEvent.Custom("premium_limit_reached", mapOf("feature_id" to "multi_vehicle"))
                        )
                        updateState { copy(showPremiumLimit = true) }
                    } else {
                        launchEffect(Effect.NavigateToAddVehicle)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadVehicles() {
        getAllContractsUseCase(GetAllContractsUseCase.Input)
            .onEach { output ->
                when (output) {
                    is GetAllContractsUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                    is GetAllContractsUseCase.Output.Success -> updateState {
                        copy(isLoading = false, vehicles = output.contracts)
                    }
                    is GetAllContractsUseCase.Output.Failure -> updateState { 
                        copy(
                            isLoading = false, 
                            error = es.joshluq.foundationkit.text.TextProvider.Resource(es.joshluq.kmsafe.R.string.history_load_error)
                        ) 
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun syncVehicles() {
        syncContractsUseCase(SyncContractsUseCase.Input).launchIn(viewModelScope)
    }

    private fun selectVehicle(id: String) {
        selectContractUseCase(SelectContractUseCase.Input(id))
            .onEach { output ->
                if (output is SelectContractUseCase.Output.Success) {
                    // Selection is reactive via loadVehicles observing the DB
                }
            }
            .launchIn(viewModelScope)
    }

    private fun deleteVehicle() {
        val vehicleId = state.value.vehicleToDelete?.id ?: return
        deleteContractUseCase(DeleteContractUseCase.Input(vehicleId))
            .onEach { output ->
                when (output) {
                    is DeleteContractUseCase.Output.Success -> {
                        analytics.track(AnalyticsEvent.Custom("vehicle_deleted"))
                        updateState { copy(vehicleToDelete = null, isLoading = false) }
                    }
                    is DeleteContractUseCase.Output.Failure -> {
                        updateState { 
                            copy(
                                isLoading = false, 
                                vehicleToDelete = null,
                                error = es.joshluq.foundationkit.text.TextProvider.Resource(es.joshluq.kmsafe.R.string.onboarding_register_error)
                            ) 
                        }
                    }
                    is DeleteContractUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                }
            }
            .launchIn(viewModelScope)
    }
}
