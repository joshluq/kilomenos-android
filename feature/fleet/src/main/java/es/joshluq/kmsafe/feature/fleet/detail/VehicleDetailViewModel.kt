package es.joshluq.kmsafe.feature.fleet.detail

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteContractUseCase
import es.joshluq.kmsafe.domain.usecase.GetVehicleByIdUseCase
import es.joshluq.kmsafe.feature.fleet.R
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import es.joshluq.kmsafe.core.ui.R as CoreR

@HiltViewModel(assistedFactory = VehicleDetailViewModel.Factory::class)
class VehicleDetailViewModel @AssistedInject constructor(
    @Assisted val vehicleId: String,
    private val getVehicleByIdUseCase: GetVehicleByIdUseCase,
    private val deleteContractUseCase: DeleteContractUseCase,
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
    private val analytics: AnalyticskitManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    @AssistedFactory
    interface Factory {
        fun create(vehicleId: String): VehicleDetailViewModel
    }

    init {
        checkPremium()
        loadVehicle()
    }

    override fun createInitialState(): State = State.Empty

    override fun handleEvent(event: Event) {
        when (event) {
            Event.OnBackClicked -> launchEffect(Effect.NavigateBack)
            Event.OnEditClicked -> launchEffect(Effect.NavigateToEdit(vehicleId))
            Event.OnDeleteClicked -> {
                val isSelected = state.value.renting?.isSelected ?: false
                if (isSelected) {
                    updateState {
                        copy(
                            error = TextProvider.Resource(R.string.onboarding_error_delete_selected)
                        )
                    }
                } else {
                    // Logic for confirmation handled in UI
                }
            }
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
                        copy(isLoading = false, error = TextProvider.Resource(CoreR.string.history_load_error))
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
