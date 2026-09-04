package es.joshluq.kmsafe.feature.profile.datamanagement

import es.joshluq.foundationkit.text.TextProvider
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.usecase.ExportDataUseCase

data class State(
    val isLoading: Boolean = false,
    val isPremium: Boolean = false,
    val showPremiumLimit: Boolean = false,
    val error: TextProvider? = null,
    val successMessage: TextProvider? = null,
    val lastExportedContent: String? = null
) : UiState {
    companion object {
        val Empty = State()
    }
}

sealed interface Event : UiEvent {
    data class OnExportClicked(val format: ExportDataUseCase.Format) : Event
    data object OnImportRequested : Event
    data class OnImportClicked(val content: String) : Event
    data object OnDismissError : Event
    data object OnUpgradeClicked : Event
    data object OnDismissPremiumLimit : Event
}

sealed interface Effect : UiEffect {
    data class CreateFile(val filename: String) : Effect
    data object LaunchImportPicker : Effect
    data object NavigateBack : Effect
    data object NavigateToPremiumPaywall : Effect
}
