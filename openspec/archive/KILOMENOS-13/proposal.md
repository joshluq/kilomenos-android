# Change Proposal: Fix: Lectura asíncrona de notificaciones, actualización de syncStatus y navegación inmediata

**Change ID**: `KILOMENOS-13`  
**Status**: Ready for Dev  
**Created At**: 2026-09-24 11:57:12  
**Authors**: Software Architect & Senior Android Developer  
**Target Modules**: `:feature:overview`, `:core:infrastructure`  

---

## 1. Intent & Business Value

### 1.1 Problem Statement
1. **Píldora de Notificación no Desaparece al Ser Pulsada**:
   En `OverviewViewModel.kt`, la observación de notificaciones utilizaba un fallback con el operador Elvis `?: output.notifications.firstOrNull()`. Cuando una notificación pasa a `isRead = true`, Room emite la lista actualizada donde no hay elementos no leídos; sin embargo, el fallback tomaba la primera notificación leída y la asignaba a `activeNotification`, manteniendo visible la alerta de forma permanente.
2. **syncStatus Permanece en PENDING en Room**:
   En `NotificationRepositoryImpl.kt` (`markAsRead`), al recibir una respuesta exitosa HTTP 200 de `notificationsApiService.markAsRead(targetId)`, no se invocaba `notificationDao.updateSyncStatus(listOf(targetId), "SYNCED")`, dejando el estado local en `PENDING` indefinidamente a pesar de estar sincronizado con el backend.
3. **Navegación Congelada por Petición de Red Síncrona**:
   Al pulsar la píldora (`OnNotificationPillClicked`), el ViewModel suspendía la ejecución esperando que `markNotificationAsReadUseCase` completase la petición de red remota antes de emitir el efecto de navegación (`NavigateToProjection` o `NavigateToOnboarding`). Esto bloqueaba la transición de pantalla si la red era lenta o no respondía de inmediato.

### 1.2 User Experience & Architectural Value
- **Optimistic UI Inmediata**: La píldora desaparece en <16ms tras el click del usuario (`activeNotification = null`).
- **Navegación Fluida Sin Espera de Red**: El efecto de navegación se emite inmediatamente sin retraso perceptivo.
- **Sincronización Coherente en Background**: La llamada de red se ejecuta de fondo en `Dispatchers.IO`, y al retornar HTTP 200 actualiza de inmediato el `syncStatus = "SYNCED"` en SQLite Room.

---

## 2. Scope of Changes
- **`:feature:overview`**:
  - `OverviewViewModel.kt`:
    - En `observeNotifications`, filtrar estrictamente por notificaciones no leídas (`status == NotificationStatus.UNREAD && !it.isRead`) y asignar `null` si no hay ninguna, eliminando el fallback `?: output.notifications.firstOrNull()`.
    - En `Event.OnNotificationPillClicked`, limpiar de inmediato `activeNotification = null`, emitir el efecto de navegación de forma síncrona/instantánea, y despachar `markNotificationAsReadUseCase` dentro de `viewModelScope.launch(Dispatchers.IO)` sin bloquear la UI.
- **`:core:infrastructure`**:
  - `NotificationRepositoryImpl.kt`:
    - En `markAsRead`, tras `response.isSuccessful`, llamar a `notificationDao.updateSyncStatus(listOf(targetId), "SYNCED")`.

---

## 3. Dependencies & Compatibility
- **Dependencies**: No se requieren nuevas librerías.
- **Breaking Changes**: Ninguno. Preserva al 100% las firmas y modelos existentes.
