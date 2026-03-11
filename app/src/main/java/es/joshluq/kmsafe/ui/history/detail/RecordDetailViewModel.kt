package es.joshluq.kmsafe.ui.history.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.di.DeleteOdometerRecord
import es.joshluq.kmsafe.di.GetOdometerRecord
import es.joshluq.kmsafe.di.IsUserPremium
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.usecase.DeleteOdometerRecordUseCase
import es.joshluq.kmsafe.domain.usecase.GetOdometerRecordUseCase
import es.joshluq.kmsafe.domain.usecase.IsUserPremiumUseCase
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
    @param:IsUserPremium private val isUserPremiumUseCase:
    @JvmSuppressWildcards FlowUseCase<IsUserPremiumUseCase.Input, IsUserPremiumUseCase.Output>
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
                state.value.record?.let { launchEffect(RecordDetailEffect.NavigateToEdit(it)) }
            }
            RecordDetailEvent.OnDeleteClicked -> updateState { copy(showDeleteConfirmation = true) }
            RecordDetailEvent.OnCancelDelete -> updateState { copy(showDeleteConfirmation = false) }
            RecordDetailEvent.OnDismissError -> updateState { copy(error = null) }
            RecordDetailEvent.OnConfirmDelete -> handleDelete()
        }
    }

    private fun checkSubscription() {
        isUserPremiumUseCase(IsUserPremiumUseCase.Input)
            .onEach { output ->
                if (output is IsUserPremiumUseCase.Output.Success) {
                    updateState { copy(isPremium = output.isPremium) }
                }
            }.launchIn(viewModelScope)
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
                    }
                    is GetOdometerRecordUseCase.Output.Failure -> {
                        updateState {
                            copy(
                                isLoading = false,
                                error = TextProvider.Resource(R.string.history_load_error)
                            )
                        }
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun calculateConsumption(record: OdometerRecord, previous: OdometerRecord?): Double? {
        if (previous == null || record.fuelAmount == null) return null
        val distance = record.odometerValue - previous.odometerValue
        if (distance <= 0) return null
        return (record.fuelAmount / distance.toDouble()) * 100.0
    }

    private fun handleDelete() {
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
                                    showDeleteConfirmation = false,
                                    error = TextProvider.Resource(R.string.history_register_error)
                                )
                            }
                        }
                    }
                }.launchIn(viewModelScope)
        }
    }
}
