# Delta Specification: Mejoras en el Comportamiento y Lectura de Notificaciones
**Domain**: `KILOMENOS-12` / `FEAT-005`

## Added Requirements

### REQ-KILOMENOS-12-001: Simplificación Unificada de NotificationPill
El componente `NotificationPill` no debe incluir el botón secundario "Ver más".
- **Given** una notificación activa visible en `OverviewScreen`
- **When** se renderiza `NotificationPill`
- **Then** el componente cuenta con un único área de click en su contenedor raíz y su callback `onPillClick: (Notification) -> Unit`.

### REQ-KILOMENOS-12-002: Navegación Directa y Marcado Automático de Proyección
Al pulsar sobre una notificación de proyección, la app navega a `ProjectionAnalysisScreen` y marca la alerta como leída.
- **Given** una notificación de topic `PROJECTION` en la cabecera
- **When** el usuario hace click en `NotificationPill`
- **Then** el sistema ejecuta `markNotificationAsReadUseCase` y lanza el efecto `NavigateToProjection`.

### REQ-KILOMENOS-12-003: Navegación Directa y Marcado Automático de Bluetooth
Al pulsar sobre una notificación de Bluetooth no configurado, la app navega a `EditContractScreen` y marca la alerta como leída.
- **Given** una notificación de topic `SYSTEM` con `deduplication_key` de Bluetooth faltante
- **When** el usuario hace click en `NotificationPill`
- **Then** el sistema ejecuta `markNotificationAsReadUseCase` y lanza el efecto `NavigateToOnboarding(vehicleId, isEdit = true)`.

### REQ-KILOMENOS-12-004: Nuevo Caso de Uso PublishNotificationIfUnreadUseCase
Se crea `PublishNotificationIfUnreadUseCase` para validar si la notificación enviada hace referencia a una ya leída mediante su clave semántica en Room.
- **Given** una notificación generada por `OverviewViewModel` con clave semántica en `data`
- **When** se evalúa `PublishNotificationIfUnreadUseCase`
- **Then** si ya existe en Room una entidad con esa misma clave semántica en estado leída (`isRead == true`), la llamada a `PublishNotificationUseCase` se cancela; en caso contrario, se ejecuta la publicación.

### REQ-KILOMENOS-12-005: Deprecación de setShowProjectionBanner
Confirmar la innecesariedad de `setShowProjectionBanner` en `PreferencesDataSource` y marcarlo como `@Deprecated`.
- **Given** el flag `showProjectionBanner` en `PreferencesDataSource`
- **When** se inicializa `OverviewScreen`
- **Then** la interfaz no condiciona la visibilidad de alertas a dicho flag.
