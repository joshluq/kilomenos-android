# Architecture & Technical Design: Mejoras en el Comportamiento y Lectura de Notificaciones
**Change ID**: `KILOMENOS-12`  
**Feature ID**: `FEAT-005`  
**Architectural Standards**: Clean Architecture, UDF, 4-Layer Jetpack Compose, CanvasKit Tokens  

## 1. Presentation & UI Layer
### 1.1 NotificationPill Refactoring
- En `NotificationPill.kt`, se retira el botón secundario "Ver más" (`onViewAllClick`).
- Todo el componente actúa como un único surface clicable hacia `onPillClick: (Notification) -> Unit`, con target de accesibilidad $\ge 48\times 48$dp.

### 1.2 OverviewTopBar NavigationIcon
- En `OverviewScreen.kt`, se suministra a `CanvasKitTopBar` un `navigationIcon` con el icono de campana de notificaciones.
- Al hacer click en el icono, se envía `Event.OnViewAllNotificationsClicked`, lanzando el efecto `Effect.NavigateToNotificationsList`.

### 1.3 Redirección Contextual en OverviewViewModel
- Al procesar `Event.OnNotificationPillClicked(notification)`:
  - Marca la notificación como leída mediante `markNotificationAsReadUseCase(Input(notification.id))`.
  - Evalúa `notification.topic`:
    - `PROJECTION` $\rightarrow$ `Effect.NavigateToProjection`
    - `SYSTEM` (Bluetooth) $\rightarrow$ `Effect.NavigateToOnboarding(vehicleId = contractId, isEdit = true)`
    - Otros $\rightarrow$ `Effect.NavigateToDeepLink` o `Effect.NavigateToNotificationDetail`

## 2. Domain & UseCase Layer
### 2.1 PublishNotificationIfUnreadUseCase
- Interfaz en `:core:domain` para condicionar la publicación a no-leídos.
- Implementación en `:core:infrastructure` (`PublishNotificationIfUnreadUseCaseImpl`):
  - Extrae `projection_key` o `deduplication_key` del mapa `data`.
  - Si la alerta existe en Room y su estado es `isRead == true` o `status == "READ"`, retorna `Output.SkippedAlreadyRead` sin invocar `PublishNotificationUseCase`.
  - Si no existe o no ha sido leída, invoca `PublishNotificationUseCase` y retorna `Output.Published`.

### 2.2 Limpieza de setShowProjectionBanner
- Eliminación de `setShowProjectionBanner` en `PreferencesDataSource`, repositorio y casos de uso.
- Limpieza de `showProjectionBanner` en `OverviewViewModel` y `PreferencesViewModel`.
