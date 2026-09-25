# Change Proposal: Navegación Contextual e Inmediata en Lista de Notificaciones

**Change ID**: `KILOMENOS-8`  
**Status**: Ready for Dev  
**Created At**: 2026-09-24 13:15:43  
**Authors**: Software Architect & Senior Android Developer  
**Target Modules**: `:feature:notifications`, `:app`  

---

## 1. Intent & Business Value

### 1.1 Problem Statement
1. **Navegación Errónea a Pantalla en Blanco**: Al hacer click en una notificación de tipo `PROJECTION` en la lista, el sistema navega a `NotificationDetailScreen`. Esta pantalla no ofrece herramientas para analizar o recalcular el kilometraje y puede resultar en una vista en blanco para avisos locales. El destino correcto es `ProjectionAnalysisScreen`.
2. **Ausencia de Flujo Directo de Bluetooth**: Las alertas de sistema que informan de la ausencia de Bluetooth configurado deben dirigir al usuario a la pantalla de edición del vehículo/contrato (`Destination.Onboarding(vehicleId, isEdit = true)`).
3. **Bloqueo por Sincronización Remota**: `handleNotificationClick` suspendía esperando a que `markNotificationAsReadUseCase` terminase antes de emitir efectos de navegación.
4. **Desacoplamiento Temporal de NotificationDetail**: No existe suficiente información extendida en los avisos para justificar una pantalla intermedia de detalle.

### 1.2 UX Value
- Navegación contextual directa en un solo toque (0 fricción).
- Optimistic UI con respuesta inmediata en el hilo principal (< 16ms).
- Sincronización de lectura en segundo plano en `Dispatchers.IO`.

---

## 2. Scope of Changes
- **`:feature:notifications`**:
  - `Contract.kt` (`ui/list`): Añadir `NavigateToProjection` y `NavigateToEditContract(vehicleId: String)` a `NotificationsListEffect`.
  - `NotificationsListViewModel.kt`: En `handleNotificationClick(notification)`, emitir el efecto contextual correspondiente inmediatamente y ejecutar `markNotificationAsReadUseCase` de forma asíncrona en `viewModelScope.launch(Dispatchers.IO)`.
  - `NotificationsListRoute.kt`: Agregar callbacks `onNavigateToProjection: () -> Unit` y `onNavigateToEditContract: (String) -> Unit`.
- **`:app`**:
  - `AppNavigation.kt`: Conectar en `Destination.NotificationsList` los nuevos callbacks redirigiendo a `Destination.ProjectionAnalysis` y `Destination.Onboarding(vehicleId, isEdit = true)`.

---

## 3. Dependencies & Compatibility
- Preserva retrocompatibilidad con FoundationKit y MVI.
- Sin cambios en librerías externas.
