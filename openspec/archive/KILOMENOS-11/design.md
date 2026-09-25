# Architecture & Technical Design: Formato Canónico UUID v4 y Deduplicación de Notificaciones
**Change ID**: `KILOMENOS-11`  
**Feature ID**: `FEAT-004`  
**Architectural Standards**: Clean Architecture, UDF, Local-First Room Persistence, Defensive Integration  

## 1. Domain & Persistence Architecture
### 1.1 Canonical UUID v4 Standard
- En la instanciación de cualquier notificación en Android, la propiedad `id` de `Notification` se genera estrictamente mediante `java.util.UUID.randomUUID().toString()`.
- Expresión regular de conformidad: `^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$`.

### 1.2 Encapsulación Semántica en Data
- Las claves compuestas de negocio (`proj_${contractId}_${todayEpochDay}` y `bt_missing_${contractId}`) se trasladan a los metadatos de la notificación:
  ```kotlin
  data = mapOf(
      "projection_key" to "proj_${contract.id}_$todayEpochDay",
      "contract_id" to contract.id,
      "balance_km" to balance
  )
  ```
- En Room, la columna `dataJson` de `NotificationEntity` persiste este mapa serializado como JSON.

### 1.3 Deduplicación Semántica en Repositorio y Room
- En `NotificationDao`, se añade una consulta para buscar notificaciones activas por su clave semántica en `dataJson`:
  ```kotlin
  @Query("SELECT * FROM notifications WHERE dataJson LIKE '%' || :semanticKey || '%' AND (status = 'UNREAD' OR isRead = 0) LIMIT 1")
  suspend fun findBySemanticKey(semanticKey: String): NotificationEntity?
  ```
- En `NotificationRepositoryImpl.insertOrUpdate(notification)`:
  - Si la notificación entrante contiene una clave semántica (`projection_key` o `deduplication_key`) y ya existe un registro activo en Room para ella, se reutiliza el `id` (UUID) existente para actualizar sus campos (por ejemplo título, cuerpo, fecha) sin generar una tarjeta duplicada en la interfaz del usuario.

## 2. API Contract & Defensive Integration
### 2.1 Remote API Conformance
- Al llamar a `PATCH /v1/notifications/{id}/read` y `DELETE /v1/notifications/{id}`, el parámetro de ruta siempre es un UUID canónico, satisfaciendo el tipo `UUID` de PostgreSQL en Supabase.
- Al invocar `POST /v1/notifications/sync`, `NotificationSyncItemDto` incluye `id: String` (UUID) y `data: Map<String, Any>?`, sincronizando el payload de metadatos al backend.

### 2.2 Defensive Safety Guard para IDs Heredados
- En `NotificationRepositoryImpl`:
  ```kotlin
  private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
  private fun isCanonicalUuid(id: String): Boolean = UUID_REGEX.matches(id)
  ```
  - En `markAsRead(id)` y `delete(id)`: La base de datos local en Room siempre se actualiza de inmediato. Si `isCanonicalUuid(id)` es `false`, se omite la llamada de red remota para no provocar el error HTTP 500 en PostgreSQL.

## 3. Module Targets & Code Artifacts
- `:feature:overview`: Actualización de `OverviewViewModel` para generar UUID v4 y propagar `data`.
- `:core:domain`: Preservación de `Notification.data: Map<String, Any>?`.
- `:core:infrastructure`:
  - `NotificationEntity.kt`: Serialización y deserialización bidireccional entre `Map<String, Any>?` y `dataJson: String?`.
  - `NotificationDao.kt`: Consulta `findBySemanticKey`.
  - `NotificationRepositoryImpl.kt`: Deduplicación semántica previa a inserción, propagación de `data` en sincronización por lotes, y guardas defensivas ante identificadores no-UUID.
