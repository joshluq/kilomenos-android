# KILOMENOS-10: Integración Local-First de Notificaciones y Sincronización Remota

## 1. Resumen Ejecutivo y Objetivos de Producto
Implementar en la aplicación Android (KmSafe) la arquitectura integral de notificaciones **Local-First** (Room Database + Jetpack Compose + WorkManager), permitiendo al usuario recibir, consultar y gestionar alertas locales (recordatorios, telemetría y desvíos de kilometraje) y remotas (eventos de suscripción, avisos de sistema).

Sincronizar en segundo plano de forma bidireccional e idempotente con los servicios REST desplegados en Supabase Edge Functions (**KILOMENOS-9**), respetando de forma no bloqueante las restricciones de acceso por plan (Free vs Pro) y procesando eventos push FCM HTTP v1.

## 2. Rol Responsable y Módulos
- **Rol Primario**: `senior_android_developer` / KmSafe Mobile Team
- **Rol Supervisor**: `product_owner` (`po-digital-experience-fintech`)
- **Módulos Android Afectados**: `:feature:notifications`, `:core:database`, `:core:network`, `:core:infrastructure`, `:core:domain`, `:core:model`

## 3. Personas, Comportamiento UX y Estrategia de Crecimiento
- **Conductor Particular (Free)**: Consulta sus alertas locales con 0ms de latencia offline. Al intentar sincronizar historial remoto, experimenta un banner contextual informativo que presenta los beneficios de KiloMenos Pro sin bloquear la app ni ocultar avisos locales.
- **Conductor de Renting / Gestor de Flota (Pro)**: Mantiene sincronizado el historial de avisos y estados de lectura entre múltiples terminales de cabina y móviles personales.
- **Hipótesis de Crecimiento**:
  - Reducción del Churn D30 en un +12% al mantener el historial accesible y contextual.
  - Incremento del +18% en la conversión Freemium-to-Pro mediante banner no intrusivo ante `403 PREMIUM_REQUIRED`.
  - **ICE Score**: Impact (8) × Confidence (9) × Ease (8) = 576.

## 4. Historias de Usuario (INVEST)
- **US-01**: Como conductor con o sin conexión, quiero consultar mis notificaciones inmediatamente al abrir la app, para estar al tanto de mis alertas sin retrasos de red.
- **US-02**: Como usuario, quiero que al marcar una o todas mis notificaciones como leídas, el cambio se refleje al instante en la UI (Optimistic UI) y se sincronice en segundo plano.
- **US-03**: Como conductor con plan Free, quiero ver un banner explicativo sobre KiloMenos Pro cuando se intente sincronizar el historial remoto, pudiendo seguir consultando mis alertas locales con total normalidad.
- **US-04**: Como usuario con múltiples dispositivos, quiero que las notificaciones generadas localmente se sincronicen de forma segura y desduplicada al backend al recuperar red.
- **US-05**: Como suscriptor, quiero que al recibir un push FCM con `REFRESH_ENTITLEMENTS` se actualicen mis derechos de acceso en background y se me notifique en el sistema operativo.
- **US-06**: Como conductor, quiero poder eliminar notificaciones individuales de mi historial, garantizando coherencia local y remota.

## 5. Requisitos Funcionales (FR)
- **FR-01**: [Persistencia Room Local-First] Actualizar `NotificationEntity` con `id` (UUID), `userId`, `origin` (`LOCAL`/`REMOTE`), `topic`, `title`, `body`, `data` (JSON), `deepLink`, `isRead`, `readAt`, `createdAt` y `syncStatus` (`PENDING`/`SYNCED`).
- **FR-02**: [Cliente REST Supabase] Integrar cliente HTTP en `:core:network` para los 5 endpoints v1 de Supabase con cabeceras `Authorization: Bearer <jwt>`, `apikey` y `Content-Type: application/json`.
- **FR-03**: [Sincronización en Background WorkManager] Implementar `SyncNotificationsWorker` con restricciones `NetworkType.CONNECTED` para sincronizar lotes pendientes y descargar nuevas remotas.
- **FR-04**: [Optimistic UI] Aplicar mutaciones locales en Room de inmediato antes de disparar la llamada remota para lectura y borrado.
- **FR-05**: [Tratamiento Pedagógico de Plan Free] Capturar HTTP 403 `PREMIUM_REQUIRED` mostrando un banner Pro sin alterar el flujo ni lanzar excepciones en la UI.
- **FR-06**: [Idempotencia y Desduplicación] Sincronización remota basada en UUID generado por cliente con `ON CONFLICT (id) DO UPDATE`.
- **FR-07**: [Procesamiento Push FCM] Ingesta de mensajes push HTTP v1 con invalidación de caché ante `REFRESH_ENTITLEMENTS` y publicación de alertas de sistema en Android.

## 6. Criterios de Aceptación (Given / When / Then)
- **AC-01**: [Visualización Inmediata Local-First Offline]
  - **Given** que el usuario abre `NotificationsListScreen` sin conexión a internet activa,
  - **When** la pantalla se renderiza,
  - **Then** el sistema observa el flujo reactivo de Room mostrando todas las alertas cacheadas sin spinner bloqueante ni fallo.
- **AC-02**: [Sincronización en Segundo Plano con WorkManager]
  - **Given** que existen notificaciones en Room con `syncStatus = PENDING`,
  - **When** el dispositivo recupera conectividad de red y se ejecuta `SyncNotificationsWorker`,
  - **Then** el worker envía el lote a `POST /v1/notifications/sync`, actualiza a `SYNCED` en Room tras 200 OK y consulta `GET /v1/notifications`.
- **AC-03**: [Marcado como Leída con Optimistic UI]
  - **Given** una notificación no leída en `NotificationsListScreen`,
  - **When** el usuario pulsa en "Marcar como leída",
  - **Then** el estado visual cambia inmediatamente a `READ` (0ms latencia perceptible), Room actualiza `isRead = true` y `readAt = now()`, y se encola `PATCH /v1/notifications/:id/read`.
- **AC-04**: [Marcado Masivo de Notificaciones]
  - **Given** múltiples notificaciones no leídas en la lista,
  - **When** el usuario pulsa "Marcar todas como leídas",
  - **Then** todas las notificaciones pasan a estado `READ` instantáneamente en la UI y Room, y se dispara `POST /v1/notifications/read-all`.
- **AC-05**: [Manejo Resiliente de Error 403 PREMIUM_REQUIRED (Plan Free)]
  - **Given** un usuario con plan Free autenticado,
  - **When** se invoca `GET /v1/notifications` y el backend responde HTTP 403 `PREMIUM_REQUIRED`,
  - **Then** la app no se bloquea, mantiene visibles las notificaciones locales en Room y despliega un banner informativo invitando a suscribirse a Pro.
- **AC-06**: [Procesamiento de Push FCM HTTP v1 y Alerta de Sistema]
  - **Given** que la app recibe un mensaje push FCM con `origin = REMOTE`, `topic`, `title` y `body`,
  - **When** `KmFirebaseMessagingService` procesa el evento,
  - **Then** persiste la entidad en Room con `origin = REMOTE` y `syncStatus = SYNCED` y muestra una notificación en la barra de estado con su deep link.
- **AC-07**: [Refresco Reactivo de Derechos ante Push de Entitlements]
  - **Given** un push FCM con `action_code = "REFRESH_ENTITLEMENTS"`,
  - **When** el dispositivo procesa el mensaje en primer o segundo plano,
  - **Then** se invalida la caché de sesión de usuario y se dispara la sincronización de derechos de suscripción.
- **AC-08**: [Eliminación Individual de Notificación]
  - **Given** una notificación existente en el historial,
  - **When** el usuario desliza o pulsa eliminar,
  - **Then** la notificación se elimina de inmediato de Room y de la lista en pantalla y se ejecuta `DELETE /v1/notifications/:id`.

## 7. Requisitos No Funcionales (NFRs)
- **Min SDK**: 24 (Android 7.0 Nougat).
- **Target SDK**: 35 (Android 15).
- **Offline Capability**: **REQUIRED**. Room actúa como Single Source of Truth para la UI.
- **Performance Budget**: Render inicial < 16ms (60 fps), sincronización en segundo plano con impacto de batería < 2%.
- **Accesibilidad**: Touch targets >= 48x48dp, TalkBack labels en todas las acciones y compatibilidad con Dynamic Type al 200%.
- **Seguridad**: TLS 1.3 en todas las comunicaciones, tokens Bearer y API key en cabeceras HTTP, cero PII o tokens en Logcat.

## 8. Ambientes y Contratos de Servicios REST (Backend API v1)
- **URL Base Supabase Edge Function**: `https://pfbdokxsmmrhrltfsnox.supabase.co/functions/v1/api`
- **Cabeceras Obligatorias**:
  - `Authorization: Bearer <user_jwt_token>`
  - `apikey: <supabase_anon_key>`
  - `Content-Type: application/json`
- **Endpoints**:
  1. `GET /v1/notifications` (Query: `limit`, `offset`, `unread_only`, `topic`). Resp: `{ success: true, data: { items: [...], total, unread_count } }`. Error 403: `{ success: false, error: "PREMIUM_REQUIRED" }`.
  2. `POST /v1/notifications/sync` (Body: `{ notifications: [ ... ] }`). Idempotente con `ON CONFLICT (id) DO UPDATE`.
  3. `PATCH /v1/notifications/:id/read`.
  4. `POST /v1/notifications/read-all`.
  5. `DELETE /v1/notifications/:id`.

## 9. Contrato Push FCM (HTTP v1)
- Payload data: `{ topic, event_type, action_code, title, body, deep_link }`.
- Si `action_code == "REFRESH_ENTITLEMENTS"`, refrescar sesión/derechos. Si incluye `title` y `body`, guardar en Room con `origin = REMOTE` y emitir notificación de sistema con `NotificationCompat.Builder`.

## 10. Telemetría y Analítica de Crecimiento
- `notification_sync_started` (`trigger_type`, `pending_count`)
- `notification_sync_completed` (`synced_count`, `duration_ms`, `success`)
- `notification_sync_failed` (`error_code`, `http_status`, `is_premium_required`)
- `notification_marked_read` (`notification_id`, `topic`, `origin`)
- `notification_all_read` (`total_read_count`)
- `notification_deleted` (`notification_id`, `topic`)
- `notification_free_banner_viewed` (`source`, `cta_type`)
- `notification_free_banner_clicked` (`source`)

## 11. Trazabilidad de Artefactos
- **Backend Feature**: KILOMENOS-9 (Supabase Endpoints - Finalizada)
- **PRD**: `docs/prd/PRD-FEAT-003.md`
- **Handoff Contract**: `handoffs/po_to_architect_FEAT-003.json`
- **OpenSpec Change**: `openspec/changes/KILOMENOS-10/`
