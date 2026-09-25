# Component Interface Specification: Corrección de Lectura Asíncrona, syncStatus y Navegación Inmediata

**Feature ID**: FEAT-006  
**Jira Issue**: KILOMENOS-13  
**Component Identifier**: NotificationAsyncReadAndNavigationFix  
**Package**: es.joshluq.kmsafe  
**Target Modules**: `:feature:overview`, `:core:infrastructure`  
**Architecture Pattern**: Clean Architecture + UDF MVI  
**Status**: APPROVED  

---

## 1. Contracts & Specifications

### 1.1 OverviewViewModel Contracts (`:feature:overview`)
Ubicado en `feature/overview/src/main/java/es/joshluq/kmsafe/feature/overview/OverviewViewModel.kt`.

#### Contrato de Selección Reactiva de Alertas Activas
```kotlin
private fun observeNotifications() {
    viewModelScope.launch(dispatcherProvider.io) {
        observeNotificationsUseCase(ObserveNotificationsUseCase.Input(limit = 10))
            .distinctUntilChanged()
            .collectLatest { output ->
                val mostRelevant = output.notifications
                    .firstOrNull { it.status == NotificationStatus.UNREAD && !it.isRead }
                updateState { copy(activeNotification = mostRelevant) }
            }
    }
}
```
**Regla de invariante**: Se elimina cualquier fallback con el operador Elvis a notificaciones leídas (`?: output.notifications.firstOrNull()`). Si no hay elementos que satisfagan `it.status == NotificationStatus.UNREAD && !it.isRead`, `activeNotification` debe ser `null`.

#### Contrato de Manejo de Click en la Píldora
```kotlin
is Event.OnNotificationPillClicked -> {
    val targetNotification = currentState.activeNotification ?: event.notification
    updateState { copy(activeNotification = null) }

    if (targetNotification.topic == NotificationTopic.PROJECTION) {
        launchEffect(Effect.NavigateToProjection)
    } else if (targetNotification.topic == NotificationTopic.SYSTEM &&
        targetNotification.data["deduplication_key"]?.contains("bt_missing") == true
    ) {
        val contractId = targetNotification.data["contract_id"] ?: currentState.vehicleInfo.contractId
        launchEffect(Effect.NavigateToOnboarding(vehicleId = contractId, isEdit = true))
    }

    viewModelScope.launch(dispatcherProvider.io) {
        runCatching {
            markNotificationAsReadUseCase(targetNotification.id)
        }
    }
}
```
**Regla de invariante**: 
- `activeNotification` se limpia de forma inmediata en el estado.
- `launchEffect(...)` se invoca de manera síncrona en el hilo principal antes de cualquier operación I/O.
- `markNotificationAsReadUseCase(...)` se lanza en segundo plano en `dispatcherProvider.io` sin suspender la navegación ni arrojar excepciones que interrumpan el flujo.

---

### 1.2 NotificationRepositoryImpl Contracts (`:core:infrastructure`)
Ubicado en `core/infrastructure/src/main/java/es/joshluq/kmsafe/infrastructure/repository/NotificationRepositoryImpl.kt`.

#### Contrato de Actualización de syncStatus
```kotlin
override suspend fun markAsRead(notificationId: String) = withContext(Dispatchers.IO) {
    val targetId = parseLocalOrRemoteId(notificationId)
    notificationDao.markAsRead(targetId)

    if (isCanonicalUuid(targetId)) {
        runCatching {
            val response = notificationsApiService.markAsRead(targetId)
            if (response.isSuccessful) {
                notificationDao.updateSyncStatus(listOf(targetId), "SYNCED")
            }
        }
    }
}
```
**Regla de invariante**: Cuando `response.isSuccessful` sea verdadero (HTTP 2xx), se debe invocar obligatoriamente `notificationDao.updateSyncStatus(listOf(targetId), "SYNCED")` para garantizar la consistencia entre el almacén local Room y la API remota.
