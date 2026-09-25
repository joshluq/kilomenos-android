# Delta Specification: Desacoplamiento de la Cancelación de Notificación de Viaje

**Domain**: `KILOMENOS-6`  
**Target Capabilities**: Tracking Notification Lifecycle & Presentation Decoupling  

---

## Added / Modified Requirements

### REQ-KILOMENOS-6-001: Desacoplamiento de Compose UI y NotificationManager
La interfaz Composable `OverviewScreen` y su jerarquía de efectos MVI no deben acceder a `NotificationManager` ni invocar cancelación de notificaciones con IDs hardcodeados.
- **Given** el módulo `:feature:overview`
- **When** se analizan `OverviewScreen.kt` y `Contract.kt`
- **Then** no existe `Effect.DismissTrackingNotifications` ni llamadas a `getSystemService(Context.NOTIFICATION_SERVICE)` con la constante 1002.

### REQ-KILOMENOS-6-002: Cancelación de Notificación al Guardar Odómetro
Al confirmar y guardar un registro de odómetro tras un viaje rastreado, el sistema debe descartar la notificación de viaje finalizado mediante la capa de tracking.
- **Given** un viaje rastreado pendiente de confirmación en Overview
- **When** el usuario guarda exitosamente el registro de kilometraje
- **Then** la notificación de viaje finalizado (`NOTIFICATION_ID_TRIP_FINISHED`) se cancela a nivel de tracking sin emitir efectos de UI hacia la vista.

### REQ-KILOMENOS-6-003: Cancelación de Notificación al Descartar Viaje
Al pulsar el botón de cancelar viaje rastreado desde la tarjeta de viaje en Overview, la notificación de viaje finalizado debe cancelarse de forma atómica y consistente.
- **Given** un viaje rastreado mostrado en Overview
- **When** el usuario pulsa en cancelar el viaje rastreado (`OnCancelTrackedTripClicked`)
- **Then** se detiene el tracking, se limpian los datos persistidos y se cancela la notificación de viaje finalizado en la capa de tracking.
