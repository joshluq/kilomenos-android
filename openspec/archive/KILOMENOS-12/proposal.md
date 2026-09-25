# Change Proposal: Mejoras en el Comportamiento y Lectura de Notificaciones
**Change ID**: `KILOMENOS-12`  
**Feature ID**: `FEAT-005`  
**Status**: Proposed  
**Created At**: 2026-09-24 10:50:00  

## 1. Intent & Business Value
Optimizar la interacción del usuario con las alertas en la pantalla principal (`OverviewScreen`):
1. Eliminar el botón "Ver más" dentro de `NotificationPill`, convirtiendo toda la píldora en un único elemento interactivo claro y accesible.
2. Navegar directamente al destino resolutivo con un solo click y marcar la notificación como leída automáticamente:
   - Topic `PROJECTION`: Navegación a `ProjectionAnalysisScreen`.
   - Topic `SYSTEM` (Bluetooth): Navegación a `EditContractScreen`.
3. Crear el nuevo caso de uso `PublishNotificationIfUnreadUseCase` en `:core:domain`, que consulta la metadata de la notificación (`projection_key` / `deduplication_key`) y evita republicar o resucitar alertas que el usuario ya ha leído y descartado.
4. Constatar la obsolescencia y marcar como `@Deprecated` el flag `setShowProjectionBanner` en `PreferencesDataSource`.

## 2. Scope of Changes
- **Target Modules**:
  - `:feature:notifications`: Simplificación de `NotificationPill.kt` eliminando el botón "Ver más".
  - `:feature:overview`: Actualización de `OverviewScreen.kt` y `OverviewViewModel.kt` para navegación contextual (`NavigateToProjection`, `NavigateToOnboarding(isEdit = true)`) y delegación en `PublishNotificationIfUnreadUseCase`.
  - `:core:domain`: Nuevo caso de uso `PublishNotificationIfUnreadUseCase` e interfaz en `:core:domain`.
  - `:core:infrastructure`: Implementación de `PublishNotificationIfUnreadUseCaseImpl` y binding en `UseCaseModule`.
  - `:core:infrastructure`: Deprecación formal de `setShowProjectionBanner` en `PreferencesDataSource`.

## 3. Dependencies & Compatibility
- **Dependencies**: AndroidX Room, FoundationKit, Navigation 3.
- **Breaking Changes**: None. La interfaz `NotificationPill` simplifica sus callbacks (`onPillClick` único).
