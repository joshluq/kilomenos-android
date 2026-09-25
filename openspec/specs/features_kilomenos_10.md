# Living Specification: KILOMENOS-10
# Delta Specification: Integración Local-First de Notificaciones y Sincronización Remota

**Domain**: `KILOMENOS-10`  
**Schema Standard**: OpenSpec Delta Specification v1.0  
**Gating**: 100% Traceability to Automated Test Cases (AC-01 through AC-08)  

---

## Added Requirements

### REQ-KILOMENOS-10-001: Visualización Inmediata Local-First Offline (AC-01)
- **Given** que el usuario accede a `NotificationsListScreen` sin conexión a internet activa.
- **When** la pantalla se renderiza.
- **Then** el sistema observa el flujo reactivo de Room (`Flow<List<NotificationEntity>>`), mostrando todas las alertas cacheadas localmente sin spinner bloqueante ni pantallas de error.

---

### REQ-KILOMENOS-10-002: Sincronización en Segundo Plano con WorkManager (AC-02)
- **Given** que existen notificaciones en Room con `syncStatus = PENDING`.
- **When** el dispositivo cuenta con conectividad de red (`NetworkType.CONNECTED`) y se ejecuta `SyncNotificationsWorker`.
- **Then** el worker envía el lote a `POST /v1/notifications/sync`, actualiza su estado a `SYNCED` en Room tras respuesta 200 OK, e invoca `GET /v1/notifications` para descargar notificaciones remotas.

---

### REQ-KILOMENOS-10-003: Marcado como Leída con Optimistic UI (AC-03)
- **Given** una notificación no leída en `NotificationsListScreen`.
- **When** el usuario pulsa en "Marcar como leída".
- **Then** el estado visual cambia inmediatamente a `READ` (0ms latencia perceptible), Room actualiza `isRead = true` y `readAt = now()`, y se encola una llamada HTTP `PATCH /v1/notifications/:id/read`.

---

### REQ-KILOMENOS-10-004: Marcado Masivo de Notificaciones (AC-04)
- **Given** múltiples notificaciones no leídas en la lista.
- **When** el usuario pulsa la acción "Marcar todas como leídas".
- **Then** todas las notificaciones pasan a estado `READ` instantáneamente en la UI y Room, y se dispara `POST /v1/notifications/read-all`.

---

### REQ-KILOMENOS-10-005: Manejo Resiliente de Error 403 PREMIUM_REQUIRED (AC-05)
- **Given** un usuario con plan Free autenticado.
- **When** `SyncNotificationsWorker` o la pantalla invoca `GET /v1/notifications` y el backend responde HTTP 403 `{"error": "PREMIUM_REQUIRED"}`.
- **Then** la aplicación no se bloquea ni muestra pantalla de error, mantiene visibles las notificaciones locales almacenadas en Room, y despliega un banner informativo contextual invitando a suscribirse a KiloMenos Pro.

---

### REQ-KILOMENOS-10-006: Procesamiento de Push FCM HTTP v1 y Alerta de Sistema (AC-06)
- **Given** que la app recibe un mensaje push FCM con `origin = REMOTE`, `topic`, `title` y `body`.
- **When** `KmFirebaseMessagingService` procesa el evento.
- **Then** persiste la entidad en Room con `origin = REMOTE` y `syncStatus = SYNCED`, y muestra una notificación en la barra de estado de Android con su icono y deep link correspondiente.

---

### REQ-KILOMENOS-10-007: Refresco Reactivo de Derechos ante Push de Entitlements (AC-07)
- **Given** un push FCM con `action_code = "REFRESH_ENTITLEMENTS"`.
- **When** el dispositivo procesa el mensaje en primer o segundo plano.
- **Then** se invalida la caché de sesión de usuario y se dispara la sincronización de derechos de suscripción de forma transparente.

---

### REQ-KILOMENOS-10-008: Eliminación Individual de Notificación (AC-08)
- **Given** una notificación existente en el historial.
- **When** el usuario desliza o pulsa eliminar.
- **Then** la notificación se elimina de inmediato de Room y de la lista en pantalla, y se ejecuta la petición HTTP `DELETE /v1/notifications/:id`.
