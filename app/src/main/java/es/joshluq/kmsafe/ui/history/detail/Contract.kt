package es.joshluq.kmsafe.ui.history.detail

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.OdometerRecord

data class RecordDetailState(
    val record: OdometerRecord? = null,
    val previousRecord: OdometerRecord? = null,
    val isLoading: Boolean = true,
    val consumptionL100km: Double? = null,
    val isPremium: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val showEditDialog: Boolean = false,
    val isEditing: Boolean = false,
    val editingOdometerValue: String = "",
    val editingLabel: String = "",
    val editingFuel: String = "",
    val error: TextProvider? = null
) : UiState {
    companion object {
        val Empty = RecordDetailState()
    }
}

sealed interface RecordDetailEvent : UiEvent {
    data object OnEditClicked : RecordDetailEvent
    data object OnDeleteClicked : RecordDetailEvent
    data object OnConfirmDelete : RecordDetailEvent
    data object OnCancelDelete : RecordDetailEvent
    data object OnDismissError : RecordDetailEvent
    data object OnBackClicked : RecordDetailEvent
    
    data object OnDismissEdit : RecordDetailEvent
    data class OnEditingOdometerChanged(val value: String) : RecordDetailEvent
    data class OnEditingLabelChanged(val value: String) : RecordDetailEvent
    data class OnEditingFuelChanged(val value: String) : RecordDetailEvent
    data object OnUpdateRecordClicked : RecordDetailEvent
}

sealed interface RecordDetailEffect : UiEffect {
    data object NavigateBack : RecordDetailEffect
}
