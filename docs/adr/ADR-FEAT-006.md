# ADR-FEAT-006: Lectura Asíncrona de Notificaciones, Actualización de syncStatus y Navegación Inmediata

**Feature ID**: FEAT-006  
**Jira Issue**: KILOMENOS-13  
**Status**: ACCEPTED  
**Deciders**: Software Architect  
**Date**: 2026-09-24  
**Technical Stakeholders**: Senior Android Developer, QA/Testing Engineer, Product Owner  

---

## 1. Context and Problem Statement
Durante la fase de integración y pruebas del módulo de notificaciones y la pantalla principal (`OverviewScreen`), se identificaron tres fallos críticos de interacción y sincronización:
1. **Píldora fija con notificaciones leídas**: En `OverviewViewModel.kt`, el flujo reactivo de notificaciones seleccionaba la alerta activa usando `firstOrNull { it.status == UNREAD && !it.isRead } ?: output.notifications.firstOrNull()`. Al marcarse una alerta como leída, la base de datos local Room emitía la lista actualizada sin elementos no leídos; sin embargo, el fallback Elvis seleccionaba la primera notificación leída y la asignaba a `activeNotification`, impidiendo que la píldora desapareciera de la pantalla.
2. **Inconsistencia de syncStatus en Room**: En `NotificationRepositoryImpl.kt:markAsRead`, tras emitirse la llamada HTTP a `notificationsApiService.markAsRead(targetId)` y retornar código 200 OK, no se actualizaba el campo `syncStatus` en Room a `"SYNCED"`. Esto provocaba que en inspección de base de datos la notificación permaneciese perpetuamente en `"PENDING"`.
3. **Navegación bloqueada por llamada de red síncrona**: Al pulsar la píldora (`OnNotificationPillClicked`), el ViewModel suspendía secuencialmente en `markNotificationAsReadUseCase(notification.id)` antes de lanzar el efecto de navegación (`launchEffect(Effect.NavigateToProjection)`). Al requerir una llamada HTTP remota, la navegación quedaba bloqueada durante cientos de milisegundos o segundos hasta obtener respuesta del servidor.

## 2. Decision Drivers
- **Optimistic UI y Cero Latencia Perceptiva**: La píldora debe ocultarse inmediatamente (`activeNotification = null`) y el efecto de navegación debe emitirse al instante (< 16ms).
- **Procesamiento de Red Asíncrono Desacoplado**: La llamada a `markNotificationAsReadUseCase` debe ejecutarse en segundo plano en una corrutina en `Dispatchers.IO` sin bloquear la interacción del usuario.
- **Filtrado Estricto de Notificaciones Activas**: Solo deben presentarse en la cabecera notificaciones genuinamente no leídas. Si no hay elementos no leídos, `activeNotification` debe ser `null`.
- **Coherencia Atómica en Base de Datos Local**: Al completarse exitosamente la petición PATCH HTTP 200 a Supabase, `notificationDao.updateSyncStatus` debe registrar `"SYNCED"` de forma inmediata.

## 3. Considered Architectural Options
1. **Opción 1: Mantener suspensión y delegar optimismo en la UI con indicador de carga**
   - *Cons*: Experiencia torpe, añade spinners innecesarios para una acción que es local-first.
2. **Opción 2: Optimistic UI con Corrutina en Background e Invocación de updateSyncStatus (Elegida)**
   - *Pros*:
     - La pantalla responde instantáneamente a la pulsación.
     - La navegación no depende de la conectividad de red.
     - `activeNotification` refleja fielmente el estado reactivo sin fallbacks engañosos.
     - La base de datos Room refleja con precisión el estado `SYNCED`.
   - *Cons*: Requiere asegurar que errores en la llamada de red en background no provoquen crashes ni alteren la navegación ya iniciada.

## 4. Decision Outcome
- **Chosen Option**: Opción 2.
- **Architecture Pattern**: `CLEAN_ARCHITECTURE` con MVI Unidirectional Data Flow.

## 5. System Topology & Implementation Strategy
- **`:feature:overview` (`OverviewViewModel.kt`)**:
  - `observeNotifications`:
    ```kotlin
    val mostRelevant = output.notifications
        .firstOrNull { it.status == NotificationStatus.UNREAD && !it.isRead }
    updateState { copy(activeNotification = mostRelevant) }
    ```
  - `OnNotificationPillClicked`:
    ```kotlin
    val targetNotification = currentState.activeNotification ?: event.notification
    updateState { copy(activeNotification = null) }
    
    // Navegación inmediata
    if (targetNotification.topic == NotificationTopic.PROJECTION) {
        launchEffect(Effect.NavigateToProjection)
    } else if (targetNotification.topic == NotificationTopic.SYSTEM && 
               targetNotification.data["deduplication_key"]?.contains("bt_missing") == true) {
        val contractId = targetNotification.data["contract_id"] ?: currentState.vehicleInfo.contractId
        launchEffect(Effect.NavigateToOnboarding(vehicleId = contractId, isEdit = true))
    }
    
    // Marcado en background desacoplado
    viewModelScope.launch(dispatcherProvider.io) {
        runCatching {
            markNotificationAsReadUseCase(targetNotification.id)
        }
    }
    ```
- **`:core:infrastructure` (`NotificationRepositoryImpl.kt`)**:
  - `markAsRead`:
    ```kotlin
    if (isCanonicalUuid(targetId)) {
        runCatching {
            val response = notificationsApiService.markAsRead(targetId)
            if (response.isSuccessful) {
                notificationDao.updateSyncStatus(listOf(targetId), "SYNCED")
            }
        }
    }
    ```

## 6. Consequences & Tradeoffs
- **Positive**:
  - La navegación es instantánea.
  - La píldora desaparece en cuanto se hace click.
  - La base de datos local queda sincronizada con el estado remoto.
- **Negative**:
  - Ninguna contrapartida negativa identificada.
