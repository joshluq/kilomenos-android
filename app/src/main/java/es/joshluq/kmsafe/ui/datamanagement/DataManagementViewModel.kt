package es.joshluq.kmsafe.ui.datamanagement

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
import es.joshluq.kmsafe.di.ExportData
import es.joshluq.kmsafe.di.ImportData
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.usecase.CheckFeatureAccessUseCase
import es.joshluq.kmsafe.domain.usecase.ExportDataUseCase
import es.joshluq.kmsafe.domain.usecase.ImportDataUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    @param:ExportData private val exportDataUseCase:
    @JvmSuppressWildcards FlowUseCase<ExportDataUseCase.Input, ExportDataUseCase.Output>,
    @param:ImportData private val importDataUseCase:
    @JvmSuppressWildcards FlowUseCase<ImportDataUseCase.Input, ImportDataUseCase.Output>,
    @param:CheckFeatureAccess private val checkFeatureAccessUseCase:
    @JvmSuppressWildcards FlowUseCase<CheckFeatureAccessUseCase.Input, CheckFeatureAccessUseCase.Output>,
    private val analytics: AnalyticskitManager,
    private val logger: LoggerKit
) : ScreenViewModel<State, Event, Effect>() {

    init {
        observeEntitlements()
    }

    override fun createInitialState(): State = State.Empty

    override fun handleEvent(event: Event) {
        logger.d("DataManagementViewModel", "Event received: $event")
        when (event) {
            is Event.OnExportClicked -> handleExport(event.format)
            Event.OnImportRequested -> handleImportRequested()
            is Event.OnImportClicked -> handleImport(event.content)
            Event.OnUpgradeClicked -> {
                analytics.track(
                    AnalyticsEvent.Custom("premium_upgrade_clicked", mapOf("source" to "data_management_limit"))
                )
                launchEffect(Effect.NavigateToPremiumPaywall)
            }
            Event.OnDismissError -> updateState { copy(error = null, successMessage = null) }
            Event.OnDismissPremiumLimit -> updateState { copy(showPremiumLimit = false) }
        }
    }

    private fun observeEntitlements() {
        checkFeatureAccessUseCase(CheckFeatureAccessUseCase.Input(Feature.CLOUD_SYNC))
            .onEach { output ->
                if (output is CheckFeatureAccessUseCase.Output.Success) {
                    updateState { copy(isPremium = output.isGranted) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleExport(format: ExportDataUseCase.Format) {
        analytics.track(AnalyticsEvent.Custom("export_clicked", mapOf("format" to format.name)))

        if ((format == ExportDataUseCase.Format.JSON) && !state.value.isPremium) {
            analytics.track(AnalyticsEvent.Custom("premium_limit_reached", mapOf("feature_id" to "json_export")))
            updateState { copy(showPremiumLimit = true) }
            return
        }

        exportDataUseCase(ExportDataUseCase.Input(format))
            .onEach { output ->
                when (output) {
                    is ExportDataUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                    is ExportDataUseCase.Output.Success -> {
                        updateState { copy(isLoading = false, lastExportedContent = output.content) }
                        val extension = if (format == ExportDataUseCase.Format.JSON) "json" else "csv"
                        val filename = "KiloMenos_Backup_${System.currentTimeMillis()}.$extension"
                        logger.d("DataManagementViewModel", "Effect launched: CreateFile")
                        launchEffect(Effect.CreateFile(filename))
                    }
                    is ExportDataUseCase.Output.Failure -> {
                        updateState {
                            copy(
                                isLoading = false,
                                error = TextProvider.Resource(R.string.profile_data_error)
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun handleImportRequested() {
        if (!state.value.isPremium) {
            analytics.track(AnalyticsEvent.Custom("premium_limit_reached", mapOf("feature_id" to "data_import")))
            updateState { copy(showPremiumLimit = true) }
        } else {
            launchEffect(Effect.LaunchImportPicker)
        }
    }

    private fun handleImport(jsonContent: String) {
        importDataUseCase(ImportDataUseCase.Input(jsonContent))
            .onEach { output ->
                when (output) {
                    is ImportDataUseCase.Output.Progress -> updateState { copy(isLoading = true) }
                    is ImportDataUseCase.Output.Success -> {
                        updateState {
                            copy(
                                isLoading = false,
                                successMessage = TextProvider.Resource(R.string.profile_import_success)
                            )
                        }
                    }
                    is ImportDataUseCase.Output.Failure -> {
                        updateState {
                            copy(
                                isLoading = false,
                                error = TextProvider.Resource(R.string.profile_data_error)
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }
}
