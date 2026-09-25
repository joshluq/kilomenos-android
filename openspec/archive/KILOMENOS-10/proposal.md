# Change Proposal: Integración Local-First de Notificaciones y Sincronización Remota

**Change ID**: `KILOMENOS-10`  
**Status**: Refined / Ready for Dev  
**Created At**: 2026-09-23 17:52:29  
**Authors**: Product Owner (`po-digital-experience-fintech`)  
**Target Modules**: `:feature:notifications`, `:core:database`, `:core:network`, `:core:infrastructure`, `:core:domain`, `:core:model`  

---

## 1. Intent & Business Value

### 1.1 Problem Statement
1. **Desincronización Multi-terminal y Pérdida de Historial**: Los conductores de renting y flotas pierden sus avisos y estados de lectura si cambian de dispositivo o reinstalan la app, al residir las notificaciones únicamente en el almacenamiento local sin sincronización en la nube.
2. **Desconexión con Supabase v1 (KILOMENOS-9)**: El backend ha completado los endpoints REST para persistencia y control de notificaciones; la app Android necesita integrarse de forma reactiva, bidireccional e idempotente.
3. **Fricción en Paywall y Control de Plan Free (HTTP 403)**: El acceso al historial remoto está restringido a suscriptores Pro. Es crítico garantizar que usuarios Free no sufran caídas ni bloqueos cuando el backend retorne `403 PREMIUM_REQUIRED`, manteniendo operativas las alertas locales de Room y mostrando un banner pedagógico de conversión.
4. **Idempotencia y Resiliencia en Zonas sin Cobertura**: Conducción en túneles o autopistas sin señal requiere una arquitectura Local-First estricta: inserción inmediata en Room (0ms latencia) y sincronización diferida en segundo plano con WorkManager.

### 1.2 Target Personas & Behavioral UX
- **Conductor Particular (Plan Free)**: Necesita consultar sus alertas de kilometraje locales con 0 fallos de red. Ante la sincronización remota restringida, experimenta un banner no intrusivo destacando el valor de la copia en la nube de KiloMenos Pro.
- **Gestor de Flota / Profesional (Plan Pro)**: Opera en múltiples terminales; requiere sincronización bidireccional inmediata, actualización de estados de lectura (`is_read`) y eliminación coherente sin registros huérfanos.

### 1.3 Growth & Value Hypothesis
- **Retención D30**: Incremento proyectado de +12% al mantener el historial de alertas accesible y conectado.
- **Conversión Free-to-Pro**: Incremento de +18% mediante el banner contextual ante 403 `PREMIUM_REQUIRED`.
- **ICE Score**: Impact (8) × Confidence (9) × Ease (8) = **576**.

---

## 2. Scope of Changes

### 2.1 Key Capabilities
- **Room Database Enrichment**: Extensión de `NotificationEntity` con `userId`, `origin` (`LOCAL`/`REMOTE`), `data` (JSON), `deepLink`, `isRead`, `readAt`, `createdAt`, y `syncStatus` (`PENDING`/`SYNCED`).
- **Supabase REST Client**: Servicio HTTP en `:core:network` para los 5 endpoints v1 (`GET`, `POST /sync`, `PATCH /:id/read`, `POST /read-all`, `DELETE /:id`).
- **WorkManager Sync Worker**: `SyncNotificationsWorker` con restricciones `NetworkType.CONNECTED` para sincronización periódica y automática por lotes.
- **Optimistic UI**: Actualización instantánea en Room y UI al marcar como leída o eliminar, propagando la mutación al backend de fondo.
- **Free Plan Handling**: Interceptación del 403 `PREMIUM_REQUIRED` para desplegar banner/paywall sin interrumpir la experiencia local.
- **FCM Push HTTP v1 Processing**: Ingesta de push remotos (`origin = REMOTE`) y ejecución de `REFRESH_ENTITLEMENTS`.

---

## 3. Dependencies & Compatibility
- **Dependencies**: AndroidX WorkManager, Room 2.6+, Retrofit / OkHttp / Ktor en `:core:network`, Firebase Cloud Messaging HTTP v1.
- **Breaking Changes**: Ninguno. Migración o recreación de esquema en Room conservando retrocompatibilidad en la capa de dominio.
