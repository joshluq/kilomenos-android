# Architecture & Technical Design: KILOMENOS-5

**Feature / Change ID**: `KILOMENOS-5`  
**Architectural Lead**: Solutions Architect (`solutions_architect`) & Android Architect (`software_architect`)  
**Standard**: FoundationKit Clean Architecture, MVI, 4-Layer Jetpack Compose  

---

## 1. MVI Component Contracts ("The Being")

### 1.1 Profile Screen MVI Contract (`:feature:profile`)
- **State**: `es.joshluq.kmsafe.feature.profile.State`
  - Añadir de forma opcional `unreadNotificationsCount: Int = 0` para mostrar badge en el item de notificaciones.
- **Event / Action Hierarchy**:
  ```kotlin
  sealed interface Event : UiEvent {
      // Existing events...
      data object OnNotificationsClicked : Event
  }
  ```
- **Effect Hierarchy**:
  ```kotlin
  sealed interface Effect : UiEffect {
      // Existing effects...
      data object NavigateToNotificationsList : Effect
  }
  ```

### 1.2 Overview Screen MVI Contract (`:feature:overview`)
- **Deprecación & Limpieza de `StatusCapsule`**:
  - Eliminar la propiedad `statusCapsule: StatusCapsuleUiModel?` de `OverviewState`.
  - Eliminar los eventos `OnStatusCapsuleClicked` y `OnDismissStatusCapsule` de `OverviewEvent`.
  - La única superficie de alertas en `OverviewScreen` pasa a ser:
    ```kotlin
    val activeNotification: Notification? = null
    ```
    renderizada mediante `NotificationPill`.

---

## 2. Domain & Application Logic ("The Doing")

### 2.1 Orquestación de Notificaciones Internas en `:feature:overview`
En lugar de calcular un `StatusCapsuleUiModel` volátil en memoria en cada render, `OverviewViewModel` monitorea las transiciones de estado:
1. **Transición de Proyección (Critical Risk)**:
   - Se compara el estado de proyección actual contra el estado anterior (`previousIsOverLimit: Boolean?`).
   - Si `previousIsOverLimit == false` y `current.isOverLimit == true`:
     ```kotlin
     publishNotificationUseCase(
         PublishNotificationUseCase.Input(
             Notification(
                 id = "proj_${contract.id}_${todayEpochDay}",
                 topic = NotificationTopic.PROJECTION,
                 title = "Alerta de exceso proyectado",
                 body = "Proyectas superar tu kilometraje contratado en $distance km.",
                 priority = NotificationPriority.CRITICAL,
                 status = NotificationStatus.UNREAD,
                 deepLinkUri = "kmsafe://feature/projection",
                 actionLabel = "Ver Proyección"
             )
         )
     )
     ```
   - Si `previousIsOverLimit == true` y `current.isOverLimit == false`:
     ```kotlin
     publishNotificationUseCase(
         PublishNotificationUseCase.Input(
             Notification(
                 id = "proj_safe_${contract.id}_${todayEpochDay}",
                 topic = NotificationTopic.PROJECTION,
                 title = "Ritmo de kilometraje recuperado",
                 body = "Tu proyección actual está dentro de los límites del contrato.",
                 priority = NotificationPriority.INFO,
                 status = NotificationStatus.UNREAD,
                 deepLinkUri = "kmsafe://feature/projection"
             )
         )
     )
     ```
2. **Alerta de Bluetooth Faltante**:
   - Si `isPremium && isAutoTrackingEnabled && renting?.bluetoothDeviceAddress == null`:
     - Emite notificación `topic = NotificationTopic.SYSTEM`, `priority = NotificationPriority.WARNING`, `deepLinkUri = "kmsafe://feature/fleet/edit?vehicleId=${renting.vehicleId}"`.

### 2.2 Reutilización de UseCases FoundationKit
- `PublishNotificationUseCase`: `UseCase<Input, Output>` (ya bound en `:core:infrastructure` `UseCaseModule.kt`).
- `ObserveActiveNotificationsUseCase`: `FlowUseCase<Input, Output>` (observa `Room` DB reactivamente).
- `MarkNotificationAsReadUseCase`: `UseCase<Input, Output>` (actualiza `status = READ`).

---

## 3. Navigation 3 Triad Protocol Integration

1. **`:core:navigation`**:
   - `Destination.NotificationsList` ya existe y está definido.
2. **`:feature:profile`**:
   - `ProfileRoute.kt` expone el callback `onNavigateToNotificationsList: () -> Unit`.
   - `ProfileViewModel.kt` lanza `Effect.NavigateToNotificationsList` al recibir `Event.OnNotificationsClicked`.
3. **`:app`**:
   - `AppNavigation.kt`: En la rama `Destination.Profile`, pasar `onNavigateToNotificationsList = { backStack.add(Destination.NotificationsList) }`.

---

## 4. UI 4-Layer Mapping (`ProfileScreen.kt` & `OverviewScreen.kt`)

### ProfileScreen (Capa 3: Acciones y Configuración)
- Se añade `SettingsItem` antes o después de "Vehículos" y "Preferencias":
  ```kotlin
  SettingsItem(
      label = stringResource(R.string.profile_notifications_option), // "Notificaciones"
      icon = Icons.Default.Notifications,
      onClick = safeClick { onEvent(Event.OnNotificationsClicked) },
      modifier = Modifier.testTag("profile_notifications_button")
  )
  ```

### OverviewScreen (Capa 1: The Pulse)
- Se retira definitivamente la llamada a `StatusCapsule(...)`.
- `NotificationPill` permanece como el único componente flotante de alertas inmediatas sobre `AeroRunwayPacingBar`.
