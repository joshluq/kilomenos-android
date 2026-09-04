package es.joshluq.kmsafe.feature.history

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.joshluq.foundationkit.coroutines.DispatcherProvider
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.ScreenViewModel
import es.joshluq.kmsafe.core.monetization.domain.MonetizationConfig
import es.joshluq.kmsafe.core.ui.R as CoreR
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.OdometerRecord
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.DeleteOdometerRecordUseCase
import es.joshluq.kmsafe.domain.usecase.GetHistoryUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistoryUseCase: GetHistoryUseCase,
    private val deleteOdometerRecordUseCase: DeleteOdometerRecordUseCase,
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
    private val monetizationConfig: MonetizationConfig,
    private val dispatchers: DispatcherProvider,
    private val logger: LoggerKit
) : ScreenViewModel<HistoryState, HistoryEvent, HistoryEffect>() {

    init {
        checkSubscription()
        loadHistory(forceRefresh = false)
        updateState { copy(adUnitId = monetizationConfig.getBannerAdUnitId()) }
    }

    override fun createInitialState(): HistoryState = HistoryState.Empty

    override fun handleEvent(event: HistoryEvent) {
        logger.d("HistoryViewModel", "Event received: $event")
        when (event) {
            is HistoryEvent.OnDeleteRecords -> handleDeleteRecords(event.records)
            is HistoryEvent.OnToggleGroupExpansion -> handleToggleGroupExpansion(event.groupTitle)
            is HistoryEvent.OnViewDetail -> {
                updateState { copy(selectedDailyRecords = event.records) }
            }
            is HistoryEvent.OnDismissDetail -> {
                updateState { copy(selectedDailyRecords = null) }
            }
            HistoryEvent.OnRefresh -> loadHistory(forceRefresh = true)
            HistoryEvent.OnDismissError -> updateState { copy(error = null) }
            is HistoryEvent.OnSearchQueryChanged -> {
                updateState { copy(searchQuery = event.query) }
                applyFilters()
            }
            is HistoryEvent.OnGroupingModeChanged -> {
                updateState { copy(groupingMode = event.mode) }
                applyFilters()
            }
            is HistoryEvent.OnRecordClicked -> {
                launchEffect(HistoryEffect.NavigateToDetail(event.record.record.id))
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

    private fun loadHistory(forceRefresh: Boolean) {
        getHistoryUseCase(GetHistoryUseCase.Input(forceRefresh = forceRefresh))
            .onEach { output ->
                when (output) {
                    is GetHistoryUseCase.Output.Progress -> {
                        if (state.value.filteredGroups.isEmpty()) {
                            updateState { copy(isLoading = true) }
                        }
                        if (forceRefresh) {
                            updateState { copy(isRefreshing = true) }
                        }
                    }

                    is GetHistoryUseCase.Output.Success -> {
                        updateState {
                            copy(
                                isLoading = false,
                                isRefreshing = false,
                                initialRecord = output.initialRecord,
                                allRecords = output.allRecords,
                                totalKms = output.totalKms,
                                totalRecordsCount = output.totalRecordsCount
                            )
                        }
                        applyFilters()
                    }

                    is GetHistoryUseCase.Output.Failure -> {
                        updateState {
                            copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = TextProvider.Resource(CoreR.string.history_load_error)
                            )
                        }
                    }

                    GetHistoryUseCase.Output.Empty -> {
                        updateState {
                            copy(
                                isLoading = false,
                                isRefreshing = false
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun applyFilters() {
        viewModelScope.launch(dispatchers.default) {
            val currentState = state.value
            val filteredList = if (currentState.searchQuery.isBlank()) {
                currentState.allRecords
            } else {
                currentState.allRecords.filter {
                    it.record.label?.contains(currentState.searchQuery, ignoreCase = true) == true
                }
            }

            val grouped = filteredList.groupBy { item ->
                val date = Date(item.record.timestamp)
                val timeZone = TimeZone.getDefault()
                when (currentState.groupingMode) {
                    HistoryGroupingMode.DAY -> {
                        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).apply {
                            this.timeZone = timeZone
                        }.format(date)
                    }
                    HistoryGroupingMode.MONTH -> {
                        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).apply {
                            this.timeZone = timeZone
                        }.format(date).replaceFirstChar { it.uppercase() }
                    }
                    HistoryGroupingMode.YEAR -> {
                        SimpleDateFormat("yyyy", Locale.getDefault()).apply {
                            this.timeZone = timeZone
                        }.format(date)
                    }
                }
            }

            updateState { copy(filteredGroups = grouped) }
        }
    }

    private fun handleToggleGroupExpansion(groupTitle: String) {
        updateState {
            val newExpanded = if (expandedGroups.contains(groupTitle)) {
                expandedGroups - groupTitle
            } else {
                expandedGroups + groupTitle
            }
            copy(expandedGroups = newExpanded)
        }
    }

    private fun handleDeleteRecords(records: List<OdometerRecord>) {
        records.forEach { record ->
            deleteOdometerRecordUseCase(DeleteOdometerRecordUseCase.Input(record))
                .onEach { output ->
                    when (output) {
                        is DeleteOdometerRecordUseCase.Output.Progress -> Unit
                        is DeleteOdometerRecordUseCase.Output.Success -> Unit
                        is DeleteOdometerRecordUseCase.Output.Failure -> {
                            updateState {
                                copy(
                                    error = TextProvider.Resource(CoreR.string.history_register_error)
                                )
                            }
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }
}
