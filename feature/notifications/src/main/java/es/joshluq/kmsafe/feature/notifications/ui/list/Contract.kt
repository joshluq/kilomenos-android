package es.joshluq.kmsafe.feature.notifications.ui.list

import androidx.compose.runtime.Immutable
import es.joshluq.foundationkit.viewmodel.UiEffect
import es.joshluq.foundationkit.viewmodel.UiEvent
import es.joshluq.foundationkit.viewmodel.UiState
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.feature.notifications.ui.model.NotificationUiItem

@Immutable
data class NotificationsListState(
    val isLoading: Boolean = false,
    val unreadCount: Int = 0,
    val selectedTopic: NotificationTopic? = null,
    val notifications: List<NotificationUiItem> = emptyList(),
    val isSyncing: Boolean = false,
    val isPremiumRequiredBannerVisible: Boolean = false,
    val errorMessage: String? = null,
    val activeVehicleId: String? = null,
    val vehicleNames: Map<String, String> = emptyMap()
) : UiState {
    companion object {
        val Empty = NotificationsListState(isLoading = true)
    }
}

sealed interface NotificationsListEvent : UiEvent {
    data object Refresh : NotificationsListEvent
    data class TopicSelected(val topic: NotificationTopic?) : NotificationsListEvent
    data class NotificationClicked(val notification: Notification) : NotificationsListEvent
    data class MarkAsReadClicked(val notificationId: String) : NotificationsListEvent
    data object MarkAllAsReadClicked : NotificationsListEvent
    data class DeleteNotificationClicked(val notificationId: String) : NotificationsListEvent
    data object DismissPremiumBanner : NotificationsListEvent
    data object UpgradeToProClicked : NotificationsListEvent
    data object DismissError : NotificationsListEvent
}

sealed interface NotificationsListEffect : UiEffect {
    data object NavigateBack : NotificationsListEffect
    data class NavigateToDetail(val notificationId: String) : NotificationsListEffect
    data class NavigateToDeepLink(val deepLinkUri: String) : NotificationsListEffect
    data object NavigateToPaywall : NotificationsListEffect
    data object NavigateToProjection : NotificationsListEffect
    data class NavigateToEditContract(val vehicleId: String) : NotificationsListEffect
}

// Aliases for FoundationKit ScreenViewModel convention and backward compatibility
typealias State = NotificationsListState
typealias Event = NotificationsListEvent
typealias Effect = NotificationsListEffect
typealias NotificationsListUiState = NotificationsListState
typealias NotificationsListUiAction = NotificationsListEvent
