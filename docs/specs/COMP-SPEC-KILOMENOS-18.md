# Component Interface Specification: Diálogo de Confirmación para Eliminación de Notificación

**Feature ID**: KILOMENOS-18  
**Component Identifier**: NotificationsListScreen / NotificationsListViewModel / NotificationsListContract  
**Package**: es.joshluq.kmsafe.feature.notifications.ui.list  
**Target Module**: :feature:notifications  
**Architecture Pattern**: MVI + Clean Architecture + CanvasKit Design System  
**Status**: APPROVED  

---

## 1. MVI Contract Specification (`Contract.kt`)

### 1.1 `NotificationsListState`
Se amplía el modelo de estado inmutable para registrar la notificación seleccionada para confirmación de borrado:

```kotlin
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
    val vehicleNames: Map<String, String> = emptyMap(),
    val notificationPendingDeletion: NotificationUiItem? = null
) : UiState {
    companion object {
        val Empty = NotificationsListState(isLoading = true)
    }
}
```

### 1.2 `NotificationsListEvent`
Se añaden las acciones para confirmar y descartar el diálogo modal:

```kotlin
sealed interface NotificationsListEvent : UiEvent {
    data object Refresh : NotificationsListEvent
    data class TopicSelected(val topic: NotificationTopic?) : NotificationsListEvent
    data class NotificationClicked(val notification: Notification) : NotificationsListEvent
    data class MarkAsReadClicked(val notificationId: String) : NotificationsListEvent
    data object MarkAllAsReadClicked : NotificationsListEvent
    data class DeleteNotificationClicked(val notificationId: String) : NotificationsListEvent
    data object ConfirmDeleteNotificationClicked : NotificationsListEvent
    data object DismissDeleteNotificationClicked : NotificationsListEvent
    data object DismissPremiumBanner : NotificationsListEvent
    data object UpgradeToProClicked : NotificationsListEvent
    data object DismissError : NotificationsListEvent
}
```

---

## 2. ViewModel Contract Specification (`NotificationsListViewModel.kt`)

El ViewModel procesa los eventos asegurando la reactividad y la protección destructiva:

```kotlin
// Dentro de handleEvent(event: NotificationsListEvent)
when (event) {
    // ...
    is NotificationsListEvent.DeleteNotificationClicked -> {
        val itemToDelete = state.value.notifications.find { it.id == event.notificationId }
        updateState { copy(notificationPendingDeletion = itemToDelete) }
    }
    NotificationsListEvent.ConfirmDeleteNotificationClicked -> {
        val pendingItem = state.value.notificationPendingDeletion
        updateState { copy(notificationPendingDeletion = null) }
        if (pendingItem != null) {
            viewModelScope.launch {
                deleteNotificationUseCase(DeleteNotificationUseCase.Input(pendingItem.id))
            }
        }
    }
    NotificationsListEvent.DismissDeleteNotificationClicked -> {
        updateState { copy(notificationPendingDeletion = null) }
    }
    // ...
}
```

---

## 3. UI Presentation Contract (`NotificationsListScreen.kt`)

El diálogo modal se proyecta de forma declarativa sobre el Scaffold:

```kotlin
@Composable
fun NotificationsListScreen(
    uiState: NotificationsListUiState,
    onAction: (NotificationsListUiAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(...) { paddingValues ->
        // ... Contenido de la lista ...

        uiState.notificationPendingDeletion?.let {
            CanvasKitConfirmDialog(
                title = stringResource(R.string.notifications_delete_confirmation_title),
                message = stringResource(R.string.notifications_delete_confirmation_message),
                confirmText = stringResource(R.string.notifications_delete_confirm),
                cancelText = stringResource(R.string.notifications_delete_cancel),
                onConfirm = { onAction(NotificationsListEvent.ConfirmDeleteNotificationClicked) },
                onDismissRequest = { onAction(NotificationsListEvent.DismissDeleteNotificationClicked) },
                isDestructive = true,
                icon = Icons.Default.DeleteOutline
            )
        }
    }
}
```

---

## 4. Localized String Resources Contract

### `values/strings.xml`
```xml
<string name="notifications_delete_confirmation_title">¿Eliminar notificación?</string>
<string name="notifications_delete_confirmation_message">¿Estás seguro de que deseas eliminar esta notificación? Esta acción no se puede deshacer.</string>
<string name="notifications_delete_confirm">Eliminar</string>
<string name="notifications_delete_cancel">Cancelar</string>
```

### `values-en/strings.xml`
```xml
<string name="notifications_delete_confirmation_title">Delete notification?</string>
<string name="notifications_delete_confirmation_message">Are you sure you want to delete this notification? This action cannot be undone.</string>
<string name="notifications_delete_confirm">Delete</string>
<string name="notifications_delete_cancel">Cancel</string>
```
