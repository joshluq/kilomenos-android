# Change Proposal: Desacoplamiento de la Cancelación de Notificación de Viaje

**Change ID**: `KILOMENOS-6`  
**Status**: Ready for Dev  
**Created At**: 2026-09-25 18:07:57  
**Author**: Product Owner  
**Target Modules**: `:feature:overview`, `:core:domain`, `:core:tracking`  

---

## 1. Intent & Business Value

### 1.1 Problem Statement
Actualmente, la cancelación de la notificación del sistema de viaje finalizado (`NOTIFICATION_ID_TRIP_FINISHED`, ID 1002) se delega a la capa de UI dentro de `OverviewScreen.kt` a través del efecto MVI `Effect.DismissTrackingNotifications`. Esto viola los principios de Clean Architecture y la modularidad de la aplicación:
1. Compose UI accede directamente al servicio de sistema Android `NotificationManager`.
2. Se utiliza un identificador numérico hardcodeado (`1002`) en el módulo de presentación que duplica la constante interna de `:core:tracking`.
3. El ViewModel emite un efecto hacia la vista que no corresponde a una acción visual ni de navegación.

### 1.2 UX & Architectural Value
- Desacoplar completamente la capa de presentación de la gestión de notificaciones del sistema.
- Encapsular la cancelación de la notificación en los servicios/casos de uso de tracking (`:core:domain` y `:core:tracking`).
- Garantizar que al guardar el kilometraje o cancelar un viaje registrado, la notificación de viaje finalizado se elimine de forma atómica e inmediata en segundo plano (< 16ms).

---

## 2. Scope of Changes
- **`:core:domain`**:
  - Exponer la capacidad de cancelar la notificación de viaje finalizado a través de contratos de dominio (e.g. `TrackingServiceController` o un UseCase específico).
- **`:core:tracking`**:
  - Implementar la cancelación de `NOTIFICATION_ID_TRIP_FINISHED` en `TrackingServiceControllerImpl` o en la lógica de finalización de viaje.
- **`:feature:overview`**:
  - Eliminar `Effect.DismissTrackingNotifications` de `Contract.kt`.
  - Eliminar el bloque de gestión de `NotificationManager` en `OverviewScreen.kt`.
  - Actualizar `OverviewViewModel` para invocar la cancelación a nivel de dominio al guardar el odómetro o cancelar el viaje rastreado.
- **Pruebas**:
  - Actualizar `OverviewViewModelTest` y tests de dominio correspondientes.

---

## 3. Dependencies & Compatibility
- **Dependencies**: Android SDK 24+, Coroutines, Hilt.
- **Breaking Changes**: Ninguna para el usuario final. Refactorización interna de contratos de presentación y tracking.
