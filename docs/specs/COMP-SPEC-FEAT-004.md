# Component Interface Specification: Formato Canónico UUID v4 y Deduplicación Semántica

**Feature ID**: FEAT-004  
**Component Identifier**: NotificationSyncAndDeduplication  
**Package**: es.joshluq.kmsafe  
**Target Modules**: `:core:domain`, `:core:infrastructure`, `:feature:overview`, `:feature:notifications`  
**Architecture Pattern**: Clean Architecture + UDF MVI  
**Status**: APPROVED  

---

## 1. Model & Data Contracts

### 1.1 Domain Model: Notification
Ubicado en `core/domain/src/main/java/es/joshluq/kmsafe/domain/model/Notification.kt`:
```kotlin
data class Notification(
    val id: String, // Canonical UUID v4: e.g. "c3b88937-291d-4076-a05e-f007bbf90f38"
    val topic: NotificationTopic,
    val title: String,
    val body: String,
    val priority: NotificationPriority,
    val status: NotificationStatus = NotificationStatus.UNREAD,
    val deepLinkUri: String? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
    val actionLabel: String? = null,
    val userId: String = "",
    val origin: String = "LOCAL",
    val isRead: Boolean = (status == NotificationStatus.READ),
    val readAt: Long? = null,
    val createdAt: Long = timestampMillis,
    val syncStatus: String = "PENDING",
    val data: Map<String, Any>? = null
)
```

### 1.2 Entity Mapping & Data Serialization
En `core/infrastructure/src/main/java/es/joshluq/kmsafe/infrastructure/local/entity/NotificationEntity.kt`:
- Columna `dataJson: String?` serializada con Jackson ObjectMapper o formato JSON ligero.
- Funciones de extensión:
```kotlin
fun NotificationEntity.toDomain(): Notification = Notification(
    id = id,
    userId = userId,
    origin = origin,
    topic = runCatching { NotificationTopic.valueOf(topic) }.getOrDefault(NotificationTopic.SYSTEM),
    title = title,
    body = body,
    priority = runCatching { NotificationPriority.valueOf(priority) }.getOrDefault(NotificationPriority.INFO),
    status = if (isRead) NotificationStatus.READ else runCatching { NotificationStatus.valueOf(status) }.getOrDefault(NotificationStatus.UNREAD),
    deepLinkUri = deepLinkUri,
    isRead = isRead || status == "READ",
    readAt = readAt,
    createdAt = createdAt,
    timestampMillis = timestampMillis,
    actionLabel = actionLabel,
    syncStatus = syncStatus,
    data = dataJson?.let { parseJsonToMap(it) }
)

fun Notification.toEntity(): NotificationEntity = NotificationEntity(
    id = id,
    userId = userId,
    origin = origin,
    topic = topic.name,
    title = title,
    body = body,
    priority = priority.name,
    status = if (isRead) NotificationStatus.READ.name else status.name,
    deepLinkUri = deepLinkUri,
    isRead = isRead || status == NotificationStatus.READ,
    readAt = readAt,
    createdAt = createdAt,
    timestampMillis = timestampMillis,
    actionLabel = actionLabel,
    syncStatus = syncStatus,
    dataJson = data?.let { mapToJsonString(it) }
)
```

### 1.3 Local Deduplication & Safety Guard in Repository
En `NotificationRepositoryImpl`:
```kotlin
private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

fun isCanonicalUuid(id: String): Boolean = UUID_REGEX.matches(id)

override suspend fun insertOrUpdate(notification: Notification) {
    // Semantic deduplication: if data contains projection_key or deduplication_key
    val projectionKey = notification.data?.get("projection_key") as? String
        ?: notification.data?.get("deduplication_key") as? String

    if (projectionKey != null) {
        val existing = notificationDao.findBySemanticKey(projectionKey)
        if (existing != null) {
            // Update existing record preserving its original UUID
            val updated = notification.copy(id = existing.id)
            notificationDao.insertOrUpdate(updated.toEntity())
            return
        }
    }
    notificationDao.insertOrUpdate(notification.toEntity())
}

override suspend fun markAsRead(id: String) {
    notificationDao.markAsRead(id)
    if (isCanonicalUuid(id)) {
        runCatching { notificationsApiService.markAsRead(id) }
    }
}

override suspend fun delete(id: String) {
    notificationDao.deleteById(id)
    if (isCanonicalUuid(id)) {
        runCatching { notificationsApiService.deleteNotification(id) }
    }
}
```

### 1.4 Generation in OverviewViewModel
En `feature/overview/src/main/java/es/joshluq/kmsafe/feature/overview/OverviewViewModel.kt`:
```kotlin
val notif = Notification(
    id = UUID.randomUUID().toString(),
    topic = NotificationTopic.PROJECTION,
    title = "Alerta de exceso proyectado",
    body = "Tu ritmo actual proyecta superar el límite contratado en $distanceStr km.",
    priority = NotificationPriority.CRITICAL,
    status = NotificationStatus.UNREAD,
    deepLinkUri = "kmsafe://feature/projection",
    actionLabel = "Ver Proyección",
    data = mapOf(
        "projection_key" to "proj_${contract.id}_$todayEpochDay",
        "contract_id" to contract.id,
        "is_over_limit" to true
    )
)
```
Y de forma homóloga para `checkBluetoothMissingNotification`:
```kotlin
val notif = Notification(
    id = UUID.randomUUID().toString(),
    topic = NotificationTopic.SYSTEM,
    title = "Dispositivo Bluetooth no configurado",
    body = "Configura el Bluetooth de tu vehículo para habilitar el auto-tracking inteligente.",
    priority = NotificationPriority.WARNING,
    status = NotificationStatus.UNREAD,
    deepLinkUri = "kmsafe://feature/fleet/edit?vehicleId=${contract.id}",
    actionLabel = "Configurar",
    data = mapOf(
        "deduplication_key" to "bt_missing_${contract.id}",
        "contract_id" to contract.id
    )
)
```
