# ADR-FEAT-004: Estandarización Canónica UUID v4 y Deduplicación Semántica en Notificaciones

**Feature ID**: FEAT-004  
**Status**: ACCEPTED  
**Deciders**: Software Architect  
**Date**: 2026-09-23  
**Technical Stakeholders**: Senior Android Developer, QA/Testing Engineer, Product Owner  

---

## 1. Context and Problem Statement
Al marcar notificaciones como leídas mediante `PATCH /v1/notifications/{id}/read` o durante la sincronización remota por lotes en `POST /v1/notifications/sync`, la aplicación Android KmSafe enviaba identificadores de negocio compuestos como clave primaria (`proj_5a4637c8-9ddb-40b8-af7c-b15073f8090b_20719` o `bt_missing_...`). En Supabase PostgreSQL (`public.user_notifications`), la columna `id` es de tipo `UUID` estricto. La recepción de strings no conformes genera un fallo en el motor relacional: `invalid input syntax for type uuid` (HTTP 500 Internal Server Error).

Se requiere estandarizar el campo `id` como un UUID v4 canónico en todos los puntos de instanciación en Android, mover los identificadores de negocio a metadatos (`data.projection_key` / `data.deduplication_key`), implementar deduplicación semántica en Room y añadir validación defensiva en el repositorio contra IDs heredados.

## 2. Decision Drivers
- **Conformidad Estricta de Contrato Backend**: Los endpoints REST de Supabase requieren UUIDs RFC 4122 válidos.
- **Resiliencia Local-First**: La aplicación debe continuar funcionando offline y deduplicar alertas locales de proyección sin colisionar en la clave primaria.
- **Idempotencia y Sin Regresiones**: La deduplicación no debe generar tarjetas repetidas en la UI ni alterar el orden cronológico.
- **Defensa en Profundidad**: Protección contra datos legacy existentes en la base de datos de usuarios en producción.

## 3. Considered Architectural Options
1. **Opción 1: Modificar el backend para permitir strings arbitrarios en `user_notifications.id`**
   - *Pros*: Cero cambios en la generación de IDs en Android.
   - *Cons*: Rompe la arquitectura relacional en PostgreSQL, degrada el rendimiento de índices B-tree sobre UUID, vulnera el contrato de base de datos ya desplegado y requiere migraciones de backend con downtime.
2. **Opción 2: UUID v4 Canónico en Android con Clave Semántica en `data` y Deduplicación en Repositorio/Room (Elegida)**
   - *Pros*: 100% compatible con PostgreSQL UUID, respeta la separación de conceptos (identificador de entidad vs metadatos de proyección), habilita deduplicación semántica en Room y previene errores 500 en llamadas de red.
   - *Cons*: Requiere adaptar los generadores de alertas en `OverviewViewModel` y la lógica de inserción/deduplicación en `:core:infrastructure`.
3. **Opción 3: Hashear el string compuesto a UUID v5 determinista basado en namespace**
   - *Pros*: Genera un UUID válido que preserva determinismo.
   - *Cons*: Mayor complejidad criptográfica, no compatible con la convención estándar `UUID.randomUUID()` solicitada y oculta los metadatos de proyección legibles.

## 4. Decision Outcome
- **Chosen Option**: Opción 2 — UUID v4 Canónico (`java.util.UUID.randomUUID().toString()`) con encapsulación de clave semántica en `data` y deduplicación en capa de persistencia.
- **Architecture Pattern**: `CLEAN_ARCHITECTURE` con MVI Unidirectional Data Flow.
- **Justification**: Permite desacoplar la identidad única de la notificación (`id: UUID`) del contexto de negocio (`projection_key`), satisfaciendo las restricciones de PostgreSQL y manteniendo la resiliencia offline.

## 5. System Topology & Module Structure
- **`:feature:overview`**:
  - `OverviewViewModel`: Instancia `Notification` usando `id = UUID.randomUUID().toString()`.
  - Construye `data = mapOf("projection_key" to "proj_${contract.id}_$todayEpochDay", "contract_id" to contract.id, ...)` para alertas de proyección.
  - Construye `data = mapOf("deduplication_key" to "bt_missing_${contract.id}", "contract_id" to contract.id)` para alertas de Bluetooth.
- **`:core:infrastructure`**:
  - `NotificationEntity`: Serializa `data` en columna `dataJson` y expone mapeo a/desde `Notification.data`.
  - `NotificationDao`: Provee métodos para deduplicación semántica previa a la inserción.
  - `NotificationRepositoryImpl`:
    - Valida que `id` sea un UUID válido antes de despachar `PATCH /read`, `POST /sync` o `DELETE`.
    - Si un `id` legacy no es UUID, se procesa localmente en Room y se omite la llamada remota, evitando el fallo 500 en Supabase.
    - Propaga el mapa `data` en `NotificationSyncItemDto`.
- **`:core:domain`**:
  - `Notification`: Asegura inmutabilidad y presencia de `data: Map<String, Any>? = null`.

## 6. Consequences & Tradeoffs
- **Positive Consequences**:
  - Eliminación total de errores PostgreSQL 500 `invalid input syntax for type uuid`.
  - Notificaciones de proyección y Bluetooth correctamente sincronizadas en la nube.
  - Historial limpio sin tarjetas duplicadas mediante deduplicación semántica.
  - Protección activa para usuarios que actualicen con notificaciones legacy cacheadas.
- **Negative Consequences**:
  - Ligero overhead de serialización JSON en la columna `dataJson` de Room al almacenar metadatos.
