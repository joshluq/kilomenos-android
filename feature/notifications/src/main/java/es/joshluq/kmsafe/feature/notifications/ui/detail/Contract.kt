package es.joshluq.kmsafe.feature.notifications.ui.detail

import androidx.compose.runtime.Immutable
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.Notification

@Immutable
data class NotificationDetailState(
    val isLoading: Boolean = false,
    val notification: Notification? = null,
    val errorMessage: String? = null
) : UiState {
    companion object {
        val Empty = NotificationDetailState(isLoading = true)
    }
}

sealed interface NotificationDetailEvent : UiEvent {
    data object PrimaryActionClicked : NotificationDetailEvent
    data object NavigateBackClicked : NotificationDetailEvent
    data object DismissError : NotificationDetailEvent
}

sealed interface NotificationDetailEffect : UiEffect {
    data object NavigateBack : NotificationDetailEffect
    data class NavigateToDeepLink(val deepLinkUri: String) : NotificationDetailEffect
}

// Aliases for FoundationKit ScreenViewModel convention and backward compatibility
typealias State = NotificationDetailState
typealias Event = NotificationDetailEvent
typealias Effect = NotificationDetailEffect
typealias NotificationDetailUiState = NotificationDetailState
typealias NotificationDetailUiAction = NotificationDetailEvent
