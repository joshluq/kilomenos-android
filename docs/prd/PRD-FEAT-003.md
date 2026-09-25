# Product Requirements Document: Integración Local-First de Notificaciones y Sincronización Remota

**Feature ID**: FEAT-003 (Jira: KILOMENOS-10)  
**Version**: 1.0.0  
**Status**: APPROVED  
**Author**: Product Owner (Digital Experience & Fintech Growth)  
**Date**: 2026-09-23  
**Target Release**: v1.2.0  

---

## 1. Executive Summary & Problem Statement

### 1.1 Context & Problem
Actualmente, KmSafe dispone de un sistema de notificaciones local y de presentación visual (`NotificationPill`, `NotificationsListScreen`, `NotificationDetailScreen`) introducido en KILOMENOS-2 y KILOMENOS-5. Sin embargo, la persistencia reside de forma aislada en el dispositivo del usuario sin sincronización remota:
1. **Pérdida de Historial y Desincronización Multi-dispositivo**: Si un usuario reinstala la aplicación, cambia de terminal o inicia sesión en un segundo dispositivo, pierde todas sus alertas y el estado de lectura de avisos críticos de kilometraje, renting o suscripción.
2. **Desconexión con el Backend Supabase (KILOMENOS-9)**: El backend ha desplegado los servicios REST v1 para notificaciones con control de suscripción (Free vs Pro). La app móvil no sincroniza las notificaciones locales hacia la nube ni ingesta las notificaciones remotas emitidas por eventos del servidor.
3. **Manejo Abrupto del Paywall Pro (Error 403)**: El acceso al historial remoto de notificaciones está reservado para suscriptores Pro/Premium. Es imprescindible garantizar que los usuarios del plan Free puedan seguir operando la app y visualizando sus alertas locales de seguridad sin bloqueos ni caídas, transformando el error 403 `PREMIUM_REQUIRED` en una oportunidad pedagógica de conversión (Paywall banner contextual).
4. **Idempotencia y Resiliencia Offline**: Los conductores operan frecuentemente en zonas de baja o nula cobertura. Las alertas locales deben persistir de inmediato (Local-First) y sincronizarse de forma asíncrona, desduplicada y segura en segundo plano una vez restablecida la conectividad.

### 1.2 Value Proposition & Growth Hypothesis
- **Propuesta de Valor**: Ofrecer una arquitectura de notificaciones **Local-First bidireccional e idempotente** impulsada por Room Database y AndroidX WorkManager, que garantice acceso instantáneo offline (0ms latencia) y sincronización transparente con Supabase Edge Functions.
- **Hipótesis de Crecimiento (Growth & Retention)**:
  - Reducción del Churn D30 en un **12%** al mantener informados a los conductores sobre revisiones de kilometraje y vencimientos de suscripción.
  - Incremento del **18%** en la tasa de conversión Free-to-Pro mediante un banner informativo no invasivo y contextual cuando el usuario Free consulta su historial.
  - **ICE Score**: Impact (8) × Confidence (9) × Ease (8) = **576**.

---

## 2. Target Personas

- **Persona Primaria (Conductor Free)**:
  - *Contexto*: Conductor de vehículo de renting particular con plan gratuito.
  - *Dolor*: Teme que la app se bloquee o deje de avisarle cuando pierde cobertura o no tiene suscripción de pago.
  - *Comportamiento*: Utiliza la app a diario para revisar alertas locales de proyección; debe ver sus alertas locales y comprender con claridad el valor de actualizar a Pro para respaldar su historial en la nube.
- **Persona Secundaria (Conductor Pro / Fleet Manager)**:
  - *Contexto*: Gestor de flotas o conductor profesional con suscripción activa.
  - *Dolor*: Utiliza varios dispositivos (tablet de cabina y smartphone personal) y necesita sincronización bidireccional exacta de lectura/borrado de avisos sin duplicados.

---

## 3. User Stories (INVEST)

- **US-01**: Como conductor con o sin conexión a internet, quiero consultar mis notificaciones inmediatamente al abrir la aplicación, para estar al tanto de mis alertas de kilometraje y renting sin experimentar retrasos de red.
- **US-02**: Como usuario de KmSafe, quiero que cuando marque una o todas mis notificaciones como leídas, el cambio se refleje al instante en la interfaz (Optimistic UI) y se sincronice automáticamente en segundo plano con el servidor.
- **US-03**: Como conductor con plan Free, quiero ver un banner informativo explicativo sobre las ventajas de KiloMenos Pro cuando se intente sincronizar el historial remoto, pudiendo seguir consultando mis alertas locales con total normalidad y sin bloqueos.
- **US-04**: Como usuario con múltiples dispositivos, quiero que las notificaciones generadas localmente en mi terminal se envíen de forma segura y desduplicada al backend cuando recupere la conexión de red, para mantener mi historial unificado.
- **US-05**: Como suscriptor, quiero que al recibir un mensaje push FCM con código de refresco de suscripción (`REFRESH_ENTITLEMENTS`), la aplicación actualice mis derechos de acceso en segundo plano y me informe mediante una notificación local del sistema.
- **US-06**: Como conductor, quiero poder eliminar notificaciones individuales de mi historial, garantizando que desaparezcan de mi dispositivo y se marquen como eliminadas en el servidor.

---

## 4. Functional Requirements

- **FR-01**: [Persistencia Local-First Enriquecida] — El sistema actualizará `NotificationEntity` en Room para incluir: `id` (UUID v4), `userId`, `origin` (`LOCAL`/`REMOTE`), `topic`, `title`, `body`, `data` (JSON map/string), `deepLink`, `isRead`, `readAt`, `createdAt` y `syncStatus` (`PENDING`/`SYNCED`).
- **FR-02**: [Cliente de Red Supabase Edge Functions] — Se implementará un cliente HTTP REST en `:core:network` / `:core:infrastructure` configurado para consumir los endpoints v1 de Supabase (`GET /v1/notifications`, `POST /v1/notifications/sync`, `PATCH /v1/notifications/:id/read`, `POST /v1/notifications/read-all`, `DELETE /v1/notifications/:id`) con cabeceras `Authorization: Bearer <jwt>`, `apikey` y `Content-Type: application/json`.
- **FR-03**: [Trabajo en Segundo Plano con WorkManager] — Se creará `SyncNotificationsWorker` con restricciones de red conectada (`NetworkType.CONNECTED`) para ejecutar periódica y oportunistamente la sincronización por lotes de registros pendientes (`syncStatus == PENDING`) y descargar notificaciones remotas nuevas.
- **FR-04**: [Actualización Optimista de Estado de Lectura] — Al ejecutar "Marcar como leída" o "Marcar todas como leídas", el repositorio actualizará Room de inmediato en memoria y disco, emitiendo el cambio a la UI y programando la llamada remota correspondiente.
- **FR-05**: [Tratamiento Pedagógico de Plan Free (403 Forbidden)] — Al recibir un código HTTP `403` con error `PREMIUM_REQUIRED`, el repositorio capturará el error sin lanzar excepciones no controladas, notificará al estado de UI para mostrar un banner/paywall Pro, y preservará la visualización íntegra de las notificaciones locales.
- **FR-06**: [Desduplicación e Idempotencia] — La sincronización remota utilizará el identificador UUID generado por el cliente como clave única (`ON CONFLICT (id) DO UPDATE`), evitando registros duplicados ante reintentos de red.
- **FR-07**: [Procesamiento de Push FCM HTTP v1] — El receptor `KmFirebaseMessagingService` procesará el payload FCM: si `action_code == "REFRESH_ENTITLEMENTS"` invalidará la caché de sesión y refrescará derechos; si contiene `title` y `body`, persistirá la notificación en Room con `origin = REMOTE` y publicará la notificación en la bandeja del sistema operativo con `NotificationCompat.Builder`.

---

## 5. Acceptance Criteria (Given / When / Then)

### AC-01: Visualización Inmediata Local-First Offline
- **Given** que el usuario abre `NotificationsListScreen` sin conexión a internet activa,
- **When** la pantalla se renderiza,
- **Then** el sistema observa el flujo reactivo de Room (`Flow<List<NotificationEntity>>`), mostrando todas las alertas locales y remotas cacheadas sin spinner bloqueante ni pantalla de error.

### AC-02: Sincronización en Segundo Plano con WorkManager
- **Given** que existen notificaciones en Room con `syncStatus = PENDING`,
- **When** el dispositivo recupera conectividad de red (`NetworkType.CONNECTED`) y se ejecuta `SyncNotificationsWorker`,
- **Then** el worker envía el lote a `POST /v1/notifications/sync`, actualiza su estado a `SYNCED` en Room tras recibir respuesta 200 OK, y consulta `GET /v1/notifications` para descargar nuevas notificaciones remotas.

### AC-03: Marcado como Leída con Optimistic UI
- **Given** una notificación no leída en `NotificationsListScreen`,
- **When** el usuario pulsa en "Marcar como leída",
- **Then** el estado visual cambia inmediatamente a `READ` (0ms latencia perceptible), Room actualiza `isRead = true` y `readAt = now()`, y se encola una llamada HTTP `PATCH /v1/notifications/:id/read`.

### AC-04: Marcado Masivo de Notificaciones
- **Given** múltiples notificaciones no leídas en la lista,
- **When** el usuario pulsa la acción "Marcar todas como leídas",
- **Then** todas las notificaciones pasan a estado `READ` instantáneamente en la UI y Room, y se dispara `POST /v1/notifications/read-all`.

### AC-05: Manejo Resiliente de Error 403 PREMIUM_REQUIRED (Plan Free)
- **Given** un usuario con plan Free autenticado,
- **When** `SyncNotificationsWorker` o la pantalla invoca `GET /v1/notifications` y el backend responde HTTP 403 `{"error": "PREMIUM_REQUIRED"}`,
- **Then** la aplicación no se bloquea ni muestra diálogo de caída, mantiene visibles las notificaciones locales almacenadas en Room, y despliega un banner informativo contextual invitando a suscribirse a KiloMenos Pro.

### AC-06: Procesamiento de Push FCM HTTP v1 y Alerta de Sistema
- **Given** que la app recibe un mensaje push FCM con `origin = REMOTE`, `topic = subscription`, `title` y `body`,
- **When** `KmFirebaseMessagingService` procesa el evento,
- **Then** persiste la entidad en Room con `origin = REMOTE` y `syncStatus = SYNCED`, y muestra una notificación en la barra de estado de Android con su icono y deep link correspondiente.

### AC-07: Refresco Reactivo de Derechos ante Push de Entitlements
- **Given** un push FCM con `action_code = "REFRESH_ENTITLEMENTS"`,
- **When** el dispositivo procesa el mensaje en primer o segundo plano,
- **Then** se invalida la caché de sesión de usuario y se dispara la sincronización de derechos de suscripción de forma transparente.

### AC-08: Eliminación Individual de Notificación
- **Given** una notificación existente en el historial,
- **When** el usuario desliza o pulsa eliminar,
- **Then** la notificación se elimina de inmediato de Room y de la lista en pantalla, y se ejecuta la petición HTTP `DELETE /v1/notifications/:id`.

---

## 6. Non-Functional Requirements (Android Constraints)

- **Min SDK**: 24 (Android 7.0 Nougat).
- **Target SDK**: 35 (Android 15).
- **Offline Capability**: **REQUIRED**. Arquitectura Local-First integral: Room es la única fuente de verdad para la UI.
- **Performance Budget**:
  - Tiempo de render inicial de la lista < 16ms (60 fps, cero jank).
  - Ejecución de sincronización en segundo plano con impacto < 2% en batería.
  - Sobrecarga de memoria de worker y base de datos < 15MB.
- **Accessibility Standards**:
  - Targets táctiles mínimos de 48x48dp en todas las acciones interactivas.
  - Semántica TalkBack clara en estado de sincronización, banner de suscripción y botones de marcado.
  - Soporte de Dynamic Type (escalado de fuente hasta 200%).
- **Security & Privacy**:
  - Todas las peticiones HTTP remotas viajan cifradas con TLS 1.3.
  - Inclusión de cabeceras seguras `Authorization: Bearer <jwt>` y `apikey`.
  - Prohibido volcar tokens JWT, información del usuario o cuerpos sensibles en Logcat de producción.

---

## 7. Telemetry & Analytics Tracking Schema

| Evento | Parámetros Clave | Disparador |
|---|---|---|
| `notification_sync_started` | `trigger_type` ("manual", "worker", "fcm"), `pending_count` | Inicio de sincronización con backend |
| `notification_sync_completed` | `synced_count`, `duration_ms`, `success` (true/false) | Finalización exitosa de sincronización |
| `notification_sync_failed` | `error_code`, `http_status`, `is_premium_required` | Fallo o rechazo en sincronización |
| `notification_marked_read` | `notification_id`, `topic`, `origin` | Marcado individual como leída |
| `notification_all_read` | `total_read_count` | Marcado masivo de notificaciones |
| `notification_deleted` | `notification_id`, `topic` | Notificación eliminada |
| `notification_free_banner_viewed` | `source` ("notifications_history"), `cta_type` ("upgrade_pro") | Visualización del banner Pro ante 403 |
| `notification_free_banner_clicked` | `source` ("notifications_history") | Clic en el banner para ir al paywall |

---

## 8. Out of Scope

- Pasarela de pago o procesamiento de cobro in-app dentro del worker de notificaciones (la navegación se delega al módulo de suscripciones existente).
- Sincronización peer-to-peer sin pasar por los endpoints centrales de Supabase.
- Modificación de los contratos ya implementados en KILOMENOS-5 para alertas locales de odómetro y telemetría Bluetooth.
