# Architecture & Technical Design: Integración Local-First de Notificaciones y Sincronización Remota
**Change ID**: `KILOMENOS-10`  
**Architectural Standards**: Clean Architecture, Pure MVI, Offline-First (Room + WorkManager), 4-Layer Jetpack Compose  

---

## 1. MVI State & Actions ("The Being")

### 1.1 UI State Model (`NotificationsListUiState`)
```kotlin
package es.joshluq.kmsafe.feature.notifications.ui.list

import androidx.compose.runtime.Immutable
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationTopic

@Immutable
data class NotificationsListUiState(
    val notifications: List<Notification> = emptyList(),
    val selectedFilter: NotificationTopic? = null,
    val unreadCount: Int = 0,
    val isSyncing: Boolean = false,
    val isPremiumRequiredBannerVisible: Boolean = false,
    val errorMessage: String? = null
)
```

### 1.2 UI Actions Hierarchy (`NotificationsListUiAction`)
```kotlin
package es.joshluq.kmsafe.feature.notifications.ui.list

import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationTopic

sealed interface NotificationsListUiAction {
    data class OnNotificationClicked(val notification: Notification) : NotificationsListUiAction
    data class OnMarkAsReadClicked(val notificationId: String) : NotificationsListUiAction
    data object OnMarkAllAsReadClicked : NotificationsListUiAction
    data class OnDeleteNotificationClicked(val notificationId: String) : NotificationsListUiAction
    data class OnFilterSelected(val topic: NotificationTopic?) : NotificationsListUiAction
    data object OnDismissPremiumBannerClicked : NotificationsListUiAction
    data object OnUpgradeToProClicked : NotificationsListUiAction
    data object OnRefreshRequested : NotificationsListUiAction
}
```

---

## 2. Domain & Data Contracts ("The Doing")

- **Repository**: `NotificationRepository` en `:core:domain`, implementado en `:core:infrastructure` (`NotificationRepositoryImpl`).
- **UseCases**:
  - `ObserveActiveNotificationsUseCase`: Emite el flujo reactivo de Room DB filtrado o completo.
  - `MarkNotificationAsReadUseCase`: Marcado optimista en Room y encolado de llamada remota.
  - `MarkAllNotificationsAsReadUseCase`: Marcado masivo optimista.
  - `DeleteNotificationUseCase`: Eliminación local y remota.
  - `SyncNotificationsUseCase`: Invocado periódicamente por `SyncNotificationsWorker` o manualmente con `pull-to-refresh`.

---

## 3. 4-Layer Compose Presentation

- **Layer 1 (Screen / Route)**: `NotificationsListRoute` inyecta `NotificationsListViewModel`, observa `StateFlow` con `collectAsStateWithLifecycle()` y conecta canales de efectos (`Effect.NavigateToDetail`, `Effect.NavigateToProjection`, `Effect.NavigateToPaywall`).
- **Layer 2 (Coordinator)**: Maneja eventos del ciclo de vida y navegación.
- **Layer 3 (Content)**: `NotificationsListContent` renderiza la cabecera, filtros de topic, banner de suscripción KiloMenos Pro y la lista perezosa `LazyColumn`.
- **Layer 4 (Components)**: Celdas atómicas `NotificationItemCard`, `NotificationTopicChip`, `ProUpgradeBanner` con CanvasKit design tokens y touch targets >= 48dp.
