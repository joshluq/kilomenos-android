package es.joshluq.kmsafe.feature.history

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.model.RecordWithIndicator

/**
 * Modes for grouping odometer records in the history list.
 */
enum class HistoryGroupingMode {
    DAY, MONTH, YEAR
}

/**
 * Represents the UI state for the History screen.
 */
data class HistoryState(
    val initialRecord: OdometerRecord? = null,
    val allRecords: List<RecordWithIndicator> = emptyList(),
    val filteredGroups: Map<String, List<RecordWithIndicator>> = emptyMap(),
    val expandedGroups: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val totalKms: Double = 0.0,
    val totalRecordsCount: Int = 0,
    val selectedDailyRecords: List<RecordWithIndicator>? = null,
    val isPremium: Boolean = false,
    val searchQuery: String = "",
    val groupingMode: HistoryGroupingMode = HistoryGroupingMode.MONTH,
    val error: TextProvider? = null
) : UiState {
    companion object {
        val Empty = HistoryState()
    }
}

/**
 * Represents the UI events that can be triggered from the History screen.
 */
sealed interface HistoryEvent : UiEvent {
    data class OnDeleteRecords(val records: List<OdometerRecord>) : HistoryEvent
    data class OnToggleGroupExpansion(val groupTitle: String) : HistoryEvent
    data class OnViewDetail(val records: List<RecordWithIndicator>) : HistoryEvent
    data object OnDismissDetail : HistoryEvent
    data object OnRefresh : HistoryEvent
    data class OnSearchQueryChanged(val query: String) : HistoryEvent
    data class OnGroupingModeChanged(val mode: HistoryGroupingMode) : HistoryEvent
    data class OnRecordClicked(val record: RecordWithIndicator) : HistoryEvent
    data object OnDismissError : HistoryEvent
}

/**
 * Represents the side effects that can occur on the History screen.
 */
sealed interface HistoryEffect : UiEffect {
    data class NavigateToDetail(val recordId: String) : HistoryEffect
}
