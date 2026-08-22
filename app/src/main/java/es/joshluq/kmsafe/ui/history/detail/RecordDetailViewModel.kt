package es.joshluq.kmsafe.ui.history.detail

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
import es.joshluq.kmsafe.domain.di.CheckFeatureAccess
import es.joshluq.kmsafe.domain.di.DeleteOdometerRecord
import es.joshluq.kmsafe.domain.di.GetOdometerRecord
import es.joshluq.kmsafe.domain.di.GetRoute
import es.joshluq.kmsafe.domain.di.UpdateOdometerRecord
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteOdometerRecordUseCase
import es.joshluq.kmsafe.domain.usecase.GetOdometerRecordUseCase
import es.joshluq.kmsafe.domain.usecase.GetRouteUseCase
import es.joshluq.kmsafe.domain.usecase.UpdateOdometerRecordUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class RecordDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @param:GetOdometerRecord private val getOdometerRecordUseCase:
    @JvmSuppressWildcards FlowUseCase<GetOdometerRecordUseCase.Input, GetOdometerRecordUseCase.Output>,
    @param:DeleteOdometerRecord private val deleteOdometerRecordUseCase:
    @JvmSuppressWildcards FlowUseCase<DeleteOdometerRecordUseCase.Input, DeleteOdometerRecordUseCase.Output>,
    @param:UpdateOdometerRecord private val updateOdometerRecordUseCase:
    @JvmSuppressWildcards FlowUseCase<UpdateOdometerRecordUseCase.Input, UpdateOdometerRecordUseCase.Output>,
    @param:CheckFeatureAccess private val checkFeatureAccessUseCase:
    @JvmSuppressWildcards FlowUseCase<CheckFeatureAccessUseCase.Input, CheckFeatureAccessUseCase.Output>,
    @param:GetRoute private val getRouteUseCase:
    @JvmSuppressWildcards FlowUseCase<GetRouteUseCase.Input, GetRouteUseCase.Output>,
    private val analytics: AnalyticskitManager,
    private val logger: LoggerKit
) : ScreenViewModel<RecordDetailState, RecordDetailEvent, RecordDetailEffect>() {

    private val recordId: String = checkNotNull(savedStateHandle["recordId"])

    init {
        checkSubscription()
        loadRecord()
    }

    override fun createInitialState(): RecordDetailState = RecordDetailState.Empty

    override fun handleEvent(event: RecordDetailEvent) {
        when (event) {
            RecordDetailEvent.OnBackClicked -> launchEffect(RecordDetailEffect.NavigateBack)
            RecordDetailEvent.OnEditClicked -> {
                state.value.record?.let { record ->
                    updateState {
                        copy(
                            showEditDialog = true,
                            editingOdometerValue = record.odometerValue.toString(),
                            editingLabel = record.label ?: "",
                            editingFuel = record.fuelAmount?.toString() ?: ""
                        )
                    }
                }
            }
            RecordDetailEvent.OnDeleteClicked -> updateState { copy(showDeleteConfirmation = true) }
            RecordDetailEvent.OnCancelDelete -> updateState { copy(showDeleteConfirmation = false) }
            RecordDetailEvent.OnDismissError -> updateState { copy(error = null) }
            RecordDetailEvent.OnConfirmDelete -> handleDelete()

            RecordDetailEvent.OnDismissEdit -> updateState { copy(showEditDialog = false) }
            is RecordDetailEvent.OnEditingFuelChanged -> updateState { copy(editingFuel = event.value) }
            is RecordDetailEvent.OnEditingLabelChanged -> updateState { copy(editingLabel = event.value) }
            is RecordDetailEvent.OnEditingOdometerChanged -> updateState { copy(editingOdometerValue = event.value) }
            RecordDetailEvent.OnUpdateRecordClicked -> handleUpdateRecord()
            RecordDetailEvent.OnPremiumUpgradeClicked -> {
                analytics.track(
                    AnalyticsEvent.Custom("premium_upgrade_clicked", mapOf("source" to "record_detail_route_map"))
                )
                launchEffect(RecordDetailEffect.NavigateToPremiumPaywall)
            }
        }
    }

    private fun checkSubscription() {
        checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.CLOUD_SYNC))
            .onEach { output ->
                if (output is CheckFeatureAccessUseCase.Output.Success) {
                    updateState { copy(isPremium = output.isGranted) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadRecord() {
        getOdometerRecordUseCase(GetOdometerRecordUseCase.Input(recordId))
            .onEach { output ->
                when (output) {
                    is GetOdometerRecordUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                    is GetOdometerRecordUseCase.Output.Success -> {
                        val consumption = calculateConsumption(output.record, output.previousRecord)
                        updateState {
                            copy(
                                isLoading = false,
                                record = output.record,
                                previousRecord = output.previousRecord,
                                consumptionL100km = consumption
                            )
                        }

                        // Analytics for intention of use
                        if (output.record.hasRoute) {
                            if (state.value.isPremium) {
                                logger.d(
                                    "RecordDetailViewModel",
                                    "Premium user viewing record with route. Loading map..."
                                )
                                loadRoute(output.record.id)
                            } else {
                                analytics.track(
                                    AnalyticsEvent.Custom("route_teaser_viewed", mapOf("record_id" to output.record.id))
                                )
                                logger.d("RecordDetailViewModel", "Free user viewing record with route. Teaser shown.")
                            }
                        } else {
                            analytics.track(
                                AnalyticsEvent.Custom("record_no_route_viewed", mapOf("record_id" to output.record.id))
                            )
                            logger.d("RecordDetailViewModel", "Record has no route data.")
                        }
                    }
                    is GetOdometerRecordUseCase.Output.Failure -> {
                        // If we are currently deleting, ignore this error as it's likely 
                        // caused by the record being removed from DB before navigation finishes.
                        if (!state.value.isDeleting) {
                            updateState {
                                copy(
                                    isLoading = false,
                                    error = TextProvider.Resource(R.string.history_load_error)
                                )
                            }
                        }
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun loadRoute(id: String) {
        getRouteUseCase(GetRouteUseCase.Input(id))
            .onEach { output ->
                when (output) {
                    GetRouteUseCase.Output.Progress -> updateState { copy(isRouteLoading = true) }
                    is GetRouteUseCase.Output.Success -> {
                        analytics.track(
                            AnalyticsEvent.Custom(
                                "route_map_viewed",
                                mapOf(
                                    "record_id" to id,
                                    "points" to output.route.pointCount
                                )
                            )
                        )
                        updateState {
                            copy(isRouteLoading = false, route = output.route)
                        }
                    }
                    is GetRouteUseCase.Output.Failure -> updateState {
                        copy(isRouteLoading = false)
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun calculateConsumption(record: OdometerRecord, previous: OdometerRecord?): Double? {
        if (previous == null || record.fuelAmount == null) return null
        val distance = record.odometerValue - previous.odometerValue
        if (distance <= 0) return null
        return (record.fuelAmount!! / distance) * 100.0
    }

    private fun handleUpdateRecord() {
        val record = state.value.record ?: return
        val newValue = state.value.editingOdometerValue.replace(',', '.').toDoubleOrNull() ?: return
        val label = state.value.editingLabel.takeIf { it.isNotBlank() }
        val fuelAmount = state.value.editingFuel.replace(',', '.').toDoubleOrNull()

        val updatedRecord = record.copy(
            odometerValue = newValue,
            label = label,
            fuelAmount = fuelAmount
        )

        updateOdometerRecordUseCase(UpdateOdometerRecordUseCase.Input(updatedRecord))
            .onEach { output ->
                when (output) {
                    is UpdateOdometerRecordUseCase.Output.Progress -> updateState { copy(isEditing = true) }
                    is UpdateOdometerRecordUseCase.Output.Success -> {
                        if (fuelAmount != null) {
                            analytics.track(AnalyticsEvent.Custom("fuel_entry_added", mapOf("amount" to fuelAmount)))
                        }
                        updateState { copy(isEditing = false, showEditDialog = false) }
                        loadRecord() // Refresh local data
                    }
                    is UpdateOdometerRecordUseCase.Output.Failure -> {
                        updateState {
                            copy(
                                isEditing = false,
                                error = TextProvider.Resource(R.string.history_register_error)
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleDelete() {
        // Hide dialog immediately to improve UX
        updateState { copy(showDeleteConfirmation = false) }
        
        state.value.record?.let { record ->
            deleteOdometerRecordUseCase(DeleteOdometerRecordUseCase.Input(record))
                .onEach { output ->
                    when (output) {
                        is DeleteOdometerRecordUseCase.Output.Progress -> updateState { copy(isDeleting = true) }
                        is DeleteOdometerRecordUseCase.Output.Success -> {
                            launchEffect(RecordDetailEffect.NavigateBack)
                        }
                        is DeleteOdometerRecordUseCase.Output.Failure -> {
                            updateState {
                                copy(
                                    isDeleting = false,
                                    error = TextProvider.Resource(R.string.history_register_error)
                                )
                            }
                        }
                    }
                }.launchIn(viewModelScope)
        }
    }
}
