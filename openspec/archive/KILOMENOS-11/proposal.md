# Change Proposal: Formato Canónico UUID v4 y Deduplicación de Notificaciones
**Change ID**: `KILOMENOS-11`
**Feature ID**: `FEAT-004`
**Status**: Proposed
**Created At**: 2026-09-23 21:37:12

## 1. Intent & Business Value
Resolver la inconsistencia de tipos entre los identificadores de notificación generados en Android y el esquema estricto UUID de PostgreSQL en Supabase (`public.user_notifications.id`).

Actualmente, al invocar `PATCH /v1/notifications/{id}/read` o `POST /v1/notifications/sync`, se envían cadenas compuestas como `proj_5a4637c8-9ddb-40b8-af7c-b15073f8090b_20719` o `bt_missing_...`, provocando errores HTTP 500 (`invalid input syntax for type uuid`).
Estandarizar el `id` principal al formato canónico UUID v4 y mover las claves semánticas a `data.projection_key` / `data.deduplication_key` garantiza compatibilidad total con la API remota y asegura deduplicación transparente en la base de datos Room.

## 2. Scope of Changes
- **Target Modules**:
  - `:core:domain`: Modelos de notificación y soporte de claves semánticas en metadatos.
  - `:core:infrastructure`: Entidad Room `NotificationEntity`, DAO con soporte de deduplicación semántica, migración de base de datos, DTOs de red propagando `data`, y cliente defensivo ante IDs heredados no conformes.
  - `:feature:overview`: Actualización de generadores de alertas de proyección (`checkProjectionTransitionNotification`) y de configuración de Bluetooth (`checkBluetoothMissingNotification`) para usar `UUID.randomUUID().toString()` y alojar la clave semántica en `data`.
- **Key Capabilities**:
  - Generación de `id` con UUID v4 canónico (`UUID.randomUUID().toString()`).
  - Preservación de claves semánticas (`proj_${contractId}_${todayEpochDay}`) en el mapa `data["projection_key"]`.
  - Deduplicación semántica en Room evitando duplicación visual de alertas.
  - Compatibilidad de endpoints REST (`PATCH /read`, `POST /sync`, `DELETE`) con UUIDs v4 sin errores HTTP 500.

## 3. Dependencies & Compatibility
- **Dependencies**: AndroidX Room, Retrofit / OkHttp, Jackson/Moshi.
- **Breaking Changes**: None. Se mantiene compatibilidad retrospectiva defensiva ante posibles notificaciones locales preexistentes con IDs heredados.
