# ADR-FEAT-003: Arquitectura Local-First de Notificaciones, Sincronización Remota Bidireccional y Resiliencia Free/Pro

**Feature ID**: FEAT-003 (Jira: KILOMENOS-10)  
**Status**: ACCEPTED  
**Deciders**: Software Architect, Staff Android Engineer  
**Date**: 2026-09-23  
**Technical Stakeholders**: Senior Android Developer, QA/Testing Engineer, Product Owner  

---

## 1. Context and Problem Statement
El documento [PRD-FEAT-003.md](file:///c:/Users/josh_/AndroidStudioProjects/KmSafe/docs/prd/PRD-FEAT-003.md) establece los requerimientos para conectar el subsistema de notificaciones de KmSafe con el backend de Supabase Edge Functions (**KILOMENOS-9**), resolviendo los siguientes retos técnicos:

1. **Arquitectura Local-First e Idempotencia**: KmSafe opera en vehículos con cobertura intermitente. La UI debe leer reactivamente de Room Database (0ms latencia) y sincronizar hacia/desde el servidor en segundo plano mediante `SyncNotificationsWorker` (WorkManager) con desduplicación basada en UUID de cliente (`ON CONFLICT (id) DO UPDATE`).
2. **Resiliencia en Plan Free (HTTP 403 `PREMIUM_REQUIRED`)**: El backend restringe el historial remoto a suscriptores Pro. Cuando un usuario Free invoca `GET /v1/notifications`, el cliente no debe lanzar excepciones no controladas ni ocultar las alertas locales de Room, sino capturar el estado y emitir un evento para renderizar un banner Pro contextual.
3. **Optimistic UI en Lectura y Borrado**: Marcar notificaciones como leídas (`PATCH /v1/notifications/:id/read`, `POST /v1/notifications/read-all`) o eliminarlas (`DELETE /v1/notifications/:id`) debe impactar Room de inmediato y encolar llamadas remotas sin bloquear el hilo principal.
4. **Ingesta Push FCM HTTP v1**: Ingestar cargas remotas de Firebase Messaging: si `action_code == "REFRESH_ENTITLEMENTS"` se invalida la caché de sesión y refrescan los derechos de usuario; si contiene `title`/`body`, se guarda en Room con `origin = REMOTE` y emite notificación del sistema.

---

## 2. Decision Drivers
- **Offline-First Reactive Flow**: Room actúa como única fuente de verdad (SSOT). La UI nunca consume directamente DTOs remotos.
- **Unidirectional Data Flow (UDF / MVI)**: `NotificationsListViewModel` expone `StateFlow<NotificationsListUiState>` inmutable y procesa `NotificationsListUiAction`.
- **Background Concurrency via WorkManager**: `SyncNotificationsWorker` con restricciones `NetworkType.CONNECTED` y `BackoffPolicy.EXPONENTIAL`.
- **Zero Business Logic in Presentation**: Los Composables solo renderizan estados y disparan lambdas de acción.
- **Security & Zero Leak**: Headers `Authorization: Bearer <jwt>` y `apikey`. Ningún token ni PII en Logcat de producción.

---

## 3. Considered Architectural Options

### Opción 1: Network-First con Fallback en Caché (Rechazada)
- *Descripción*: La pantalla solicita primero `GET /v1/notifications` a Supabase y solo consulta Room si la petición falla.
- *Inconvenientes*: Viola el principio Local-First; introduce latencia de red perceptible (300ms - 2s); empeora la experiencia del usuario Free que recibiría 403 antes de ver sus alertas locales.

### Opción 2: Solo Sincronización Push Remota (Rechazada en KILOMENOS-5)
- *Descripción*: Las alertas locales se envían a backend y este emite push FCM para visualizarlas.
- *Inconvenientes*: Dependencia de conectividad activa; falla en zonas sin cobertura; añade costes innecesarios por llamadas a Edge Functions para cálculos locales.

### Opción 3 (Seleccionada): Local-First Híbrido con Worker Bidireccional Idempotente y Manejo Resiliente de Entitlements
- *Descripción*:
  - **Room Database (v21)**: Almacena notificaciones locales y remotas con estado de sincronización (`PENDING`, `SYNCED`).
  - **WorkManager (`SyncNotificationsWorker`)**: Sincroniza lotes pendientes a `POST /v1/notifications/sync` y recupera remotas con `GET /v1/notifications` respetando restricciones de red.
  - **Supabase Retrofit Client (`NotificationsApiService`)**: Cliente autenticado en `:core:infrastructure` para los 5 endpoints REST v1.
  - **MVI Presentation**: `NotificationsListViewModel` observa Room mediante `ObserveActiveNotificationsUseCase`, realiza mutaciones optimistas y expone `isPremiumRequiredBannerVisible` cuando el backend reporta 403.
- *Ventajas*: 0ms latencia para el usuario; 100% resiliente offline; no bloquea al usuario Free; escalable y totalmente desacoplado.

---

## 4. Decision Outcome
- **Chosen Option**: **Opción 3**
- **Architecture Pattern**: `MVI` + `CLEAN_ARCHITECTURE` + `LOCAL_FIRST_SYNC`

---

## 5. System Architecture & Component Mapping

```
 ┌────────────────────────────────────────────────────────┐
 │                   Jetpack Compose UI                   │
 │ (NotificationsListScreen + NotificationPill + ProBanner)│
 └─────────────────────────▲──────────────────────────────┘
                           │ StateFlow<NotificationsListUiState>
 ┌─────────────────────────┴──────────────────────────────┐
 │              NotificationsListViewModel                │
 └───────▲───────────────────────────────────────┬────────┘
         │ ObserveActiveNotificationsUseCase     │ MarkRead / Delete UseCases
 ┌───────┴───────────────────────────────────────▼────────┐
 │                 NotificationRepository                 │
 └───────────────▲───────────────────────▲────────────────┘
                 │                       │
      ┌──────────┴────────┐     ┌────────┴─────────────┐
      │  Room Database    │     │ SyncNotifications    │
      │  (NotificationDao)│     │ Worker (WorkManager) │
      └───────────────────┘     └────────▲─────────────┘
                                         │
                                ┌────────┴─────────────┐
                                │ NotificationsApi     │
                                │ Service (Supabase v1)│
                                └──────────────────────┘
```

---

## 6. Database Migration Plan (Room Version 20 $\rightarrow$ 21)
- Incrementar versión de `AppDatabase` a `21`.
- Crear `MIGRATION_20_21` ejecutando:
  - `ALTER TABLE notifications ADD COLUMN userId TEXT NOT NULL DEFAULT ''`
  - `ALTER TABLE notifications ADD COLUMN origin TEXT NOT NULL DEFAULT 'LOCAL'`
  - `ALTER TABLE notifications ADD COLUMN dataJson TEXT`
  - `ALTER TABLE notifications ADD COLUMN isRead INTEGER NOT NULL DEFAULT 0`
  - `ALTER TABLE notifications ADD COLUMN readAt INTEGER`
  - `ALTER TABLE notifications ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0`
  - `ALTER TABLE notifications ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'`
  - `CREATE INDEX IF NOT EXISTS index_notifications_syncStatus ON notifications(syncStatus)`
  - `CREATE INDEX IF NOT EXISTS index_notifications_userId ON notifications(userId)`
