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
}

sealed interface RecordDetailEffect : UiEffect {
    data class NavigateToEdit(val record: OdometerRecord) : RecordDetailEffect
    data object NavigateBack : RecordDetailEffect
    data class ShowError(val message: TextProvider) : RecordDetailEffect
}
