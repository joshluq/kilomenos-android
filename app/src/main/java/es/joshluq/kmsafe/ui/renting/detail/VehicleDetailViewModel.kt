package es.joshluq.kmsafe.ui.renting.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.di.CheckFeatureAccess
import es.joshluq.kmsafe.di.DeleteContract
import es.joshluq.kmsafe.di.GetVehicleById
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteContractUseCase
import es.joshluq.kmsafe.domain.usecase.GetVehicleByIdUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class VehicleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:GetVehicleById private val getVehicleByIdUseCase:
    @JvmSuppressWildcards FlowUseCase<GetVehicleByIdUseCase.Input, GetVehicleByIdUseCase.Output>,
    @param:DeleteContract private val deleteContractUseCase:
    @JvmSuppressWildcards FlowUseCase<DeleteContractUseCase.Input, DeleteContractUseCase.Output>,
    @param:CheckFeatureAccess private val checkFeatureAccessUseCase:
    @JvmSuppressWildcards FlowUseCase<CheckFeatureAccessUseCase.Input, CheckFeatureAccessUseCase.Output>,
    private val analytics: AnalyticskitManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    private val vehicleId: String = checkNotNull(savedStateHandle["vehicleId"])

    init {
        checkPremium()
        loadVehicle()
    }

    override fun createInitialState(): State = State.Empty

    override fun handleEvent(event: Event) {
        when (event) {
            Event.OnBackClicked -> launchEffect(Effect.NavigateBack)
            Event.OnEditClicked -> launchEffect(Effect.NavigateToEdit(vehicleId))
            Event.OnDeleteClicked -> updateState { copy(error = null) } // Logic for confirmation handled in UI
            Event.OnConfirmDelete -> handleDelete()
            Event.OnCancelDelete -> { /* No-op */ }
            Event.OnDismissError -> updateState { copy(error = null) }
        }
    }

    private fun checkPremium() {
        checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.CLOUD_SYNC))
            .onEach { output ->
                if (output is CheckFeatureAccessUseCase.Output.Success) {
                    updateState { copy(isPremium = output.isGranted) }
                }
            }.launchIn(viewModelScope)
    }

    private fun loadVehicle() {
        logger.d("VehicleDetailVM", "Loading vehicle: $vehicleId")
        getVehicleByIdUseCase(GetVehicleByIdUseCase.Input(vehicleId))
            .onEach { output ->
                when (output) {
                    is GetVehicleByIdUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                    is GetVehicleByIdUseCase.Output.Success -> updateState {
                        copy(isLoading = false, renting = output.contract)
                    }
                    is GetVehicleByIdUseCase.Output.Failure -> updateState {
                        copy(isLoading = false, error = TextProvider.Resource(R.string.history_load_error))
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun handleDelete() {
        deleteContractUseCase(DeleteContractUseCase.Input(vehicleId))
            .onEach { output ->
                when (output) {
                    is DeleteContractUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                    is DeleteContractUseCase.Output.Success -> {
                        analytics.track(AnalyticsEvent.Custom("vehicle_deleted", mapOf("id" to vehicleId)))
                        launchEffect(Effect.NavigateBack)
                    }
                    is DeleteContractUseCase.Output.Failure -> {
                        updateState {
                            copy(isLoading = false, error = TextProvider.Resource(R.string.onboarding_register_error))
                        }
                    }
                }
            }.launchIn(viewModelScope)
    }
}
