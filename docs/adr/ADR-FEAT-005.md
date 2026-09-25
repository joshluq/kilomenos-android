# ADR-FEAT-005: Interacción Contextual de Alertas, Navegación Directa y Filtrado Semántico de Leídos

**Feature ID**: FEAT-005  
**Status**: ACCEPTED  
**Deciders**: Software Architect  
**Date**: 2026-09-24  
**Technical Stakeholders**: Senior Android Developer, QA/Testing Engineer, Product Owner  

---

## 1. Context and Problem Statement
En `OverviewScreen`, el componente `NotificationPill` disponía de dos acciones conflictivas en un espacio reducido: pulsar la píldora y un botón secundario "Ver más". Esto generaba confusión en la interacción del usuario y dispersión de eventos. Además, al pulsar una alerta de proyección o de Bluetooth faltante, la navegación dependía de deep links genéricos o pantallas de detalle, en lugar de enrutar directamente al destino operativo (`ProjectionAnalysisScreen` o `EditContractScreen`) y marcar de inmediato la alerta como leída.

Por otro lado, cada vez que el ViewModel recalculaba proyecciones o telemetría, se intentaban publicar alertas repetidas aun cuando el usuario ya las había leído y descartado. Por último, persistía en `PreferencesDataSource` la función no utilizada `setShowProjectionBanner`.

## 2. Decision Drivers
- **Simplicidad de Interacción**: Una única área táctil en `NotificationPill` con semántica TalkBack clara y target $\ge 48$dp.
- **Acceso Directo al Historial**: Integración del icono de campana de notificaciones en el `navigationIcon` de `OverviewTopBar`.
- **Navegación Contextual Inmediata**: Enrutamiento directo al destino correspondiente (`PROJECTION` $\rightarrow$ `NavigateToProjection`, `SYSTEM` $\rightarrow$ `NavigateToOnboarding(isEdit = true)`) con marcado automático en Room (Optimistic UI).
- **Invariante de Estado de No Leídos**: Un nuevo caso de uso (`PublishNotificationIfUnreadUseCase`) para gobernar si una alerta debe publicarse o descartarse en función de si ya fue leída en Room.
- **Eliminación de Deuda Técnica**: Supresión definitiva de funciones y toggles muertos (`setShowProjectionBanner`).

## 3. Considered Architectural Options
1. **Opción 1: Mantener "Ver más" y resolver la lectura únicamente en la pantalla de destino**
   - *Pros*: No modifica la UI de `NotificationPill`.
   - *Cons*: Mantiene sobrecarga cognitiva y obliga al usuario a realizar múltiples toques para descartar la alerta.
2. **Opción 2: Simplificación de NotificationPill, NavigationIcon en TopBar, Navegación Contextual y PublishNotificationIfUnreadUseCase (Elegida)**
   - *Pros*: Máxima ergonomía en pantalla principal, target táctil unificado, resolución de alertas en 1 click, prevención definitiva de resucitación de alertas leídas mediante caso de uso de dominio, y eliminación total de código muerto.
   - *Cons*: Requiere actualizar contratos en `NotificationPill` y `OverviewViewModel`.

## 4. Decision Outcome
- **Chosen Option**: Opción 2 — Clean Architecture + MVI con simplificación de `NotificationPill`, `navigationIcon` en `OverviewTopBar`, y nuevo caso de uso `PublishNotificationIfUnreadUseCase`.
- **Architecture Pattern**: `CLEAN_ARCHITECTURE` con MVI Unidirectional Data Flow.

## 5. System Topology & Module Structure
- **`:feature:notifications`**:
  - `NotificationPill`: Remueve `onViewAllClick`. Contenedor unificado interactivo ejecutando `onPillClick(notification)`.
- **`:feature:overview`**:
  - `OverviewTopBar`: Incorpora `navigationIcon` con icono de notificaciones ejecutando `Event.OnViewAllNotificationsClicked` $\rightarrow$ `Effect.NavigateToNotificationsList`.
  - `OverviewViewModel`:
    - En `OnNotificationPillClicked`: Marca como leída y enruta según `topic`: `PROJECTION` a `NavigateToProjection`; `SYSTEM` a `NavigateToOnboarding(vehicleId, isEdit = true)`.
    - Delega la publicación de alertas en `PublishNotificationIfUnreadUseCase`.
    - Elimina lecturas de `showProjectionBanner`.
- **`:core:domain`**:
  - Nuevo contrato `PublishNotificationIfUnreadUseCase` con Input/Output (`Published`, `SkippedAlreadyRead`).
  - Limpieza de `setShowProjectionBanner` en `PreferencesRepository`, `UserPreferences` y `UpdatePreferencesUseCase`.
- **`:core:infrastructure`**:
  - Implementación `PublishNotificationIfUnreadUseCaseImpl` consultando `NotificationDao.findBySemanticKey`.
  - Eliminación de `setShowProjectionBanner` en `PreferencesDataSource` y `PreferencesRepositoryImpl`.
  - Binding en `UseCaseModule`.

## 6. Consequences & Tradeoffs
- **Positive Consequences**:
  - UI limpia, accesible y sin botones duplicados en la cabecera.
  - El usuario resuelve la alerta en un solo toque sin pasos intermedios.
  - Las alertas leídas nunca vuelven a resucitar durante recálculos en background.
  - Código base más conciso sin funciones fantasma de preferencias.
- **Negative Consequences**:
  - Pequeña refactorización en pruebas unitarias de `PreferencesViewModelTest` y `OverviewViewModelTest` para reflejar la eliminación de los parámetros deprecados.
