# Product Requirements Document: Corrección de Lectura Asíncrona, syncStatus y Navegación Inmediata de Notificaciones

**Feature ID**: FEAT-006  
**Jira Issue**: KILOMENOS-13  
**Version**: 1.0.0  
**Status**: APPROVED  
**Author**: Product Owner  
**Date**: 2026-09-24  
**Target Release**: v1.2.1  

---

## 1. Executive Summary & Problem Statement
- **Problem**:
  Se han identificado tres defectos en el ciclo de vida e interacción de notificaciones desde la pantalla principal (`OverviewScreen`):
  1. **Notificación no desaparece tras lectura**: Al pulsar la píldora de notificación (`NotificationPill`), la alerta no desaparece visualmente. La selección de `activeNotification` en `OverviewViewModel` recurre al operador Elvis `?: output.notifications.firstOrNull()`. Al marcarse como leída en la base de datos local Room, el filtro de no leídas devuelve `null`, pero el fallback selecciona la primera notificación ya leída, manteniéndola visible permanentemente.
  2. **syncStatus permanece en PENDING**: En `NotificationRepositoryImpl.kt`, `markAsRead` marca localmente la notificación con `syncStatus = PENDING`. Aunque la llamada remota a la API (`notificationsApiService.markAsRead(targetId)`) responda exitosamente con HTTP 200, el repositorio nunca invoca `notificationDao.updateSyncStatus(listOf(targetId), "SYNCED")`, dejando el estado de sincronización inconsistente.
  3. **Navegación bloqueada por llamada de red**: Al hacer click en la píldora de notificación (`OnNotificationPillClicked`), el ViewModel suspende secuencialmente la corrutina esperando que `markNotificationAsReadUseCase` finalice la petición HTTP antes de emitir el efecto de navegación (`NavigateToProjection` o `NavigateToOnboarding`). Esto provoca una experiencia de usuario bloqueada y lentitud de respuesta ante una acción inmediata.

- **Value Proposition**:
  Garantizar un comportamiento reactivo e intuitivo (Optimistic UI):
  - La píldora de notificación desaparece inmediatamente al ser pulsada y solo muestra alertas activas no leídas.
  - La navegación hacia la pantalla de análisis (`ProjectionAnalysisScreen`) o edición de vehículo (`EditContractScreen`) se despacha de manera instantánea.
  - La sincronización remota de lectura se ejecuta en segundo plano sin bloquear el hilo principal ni la transición de UI.
  - La base de datos local Room actualiza su `syncStatus` a `SYNCED` inmediatamente tras la confirmación HTTP 200 de la API.

---

## 2. Target Personas
- **Primary Persona**: Conductor y gestor de flota vehicular que requiere interactuar con agilidad en la pantalla principal sin bloqueos de interfaz ni notificaciones residuales.
- **User Pain Point**: Pulsar una alerta importante y sufrir congelamiento de la interfaz esperando a la red, y ver que la misma alerta sigue visible tras haber sido atendida.
- **Usage Frequency / Environment**: Diaria, interacción frecuente al encender el vehículo o revisar el estado del contrato.

---

## 3. User Stories
- **US-01**: Como conductor que pulsa una alerta en `NotificationPill`, quiero que la píldora desaparezca de inmediato sin mostrarme notificaciones ya leídas, para tener la certeza visual de que el aviso fue atendido.
- **US-02**: Como conductor que interactúa con un aviso de proyección o configuración, quiero que la app navegue inmediatamente a la pantalla de destino, para no sufrir demoras de red en la transición.
- **US-03**: Como gestor de flota y sistema offline-first, quiero que el estado de sincronización de la notificación pase a `SYNCED` en la base de datos local una vez que el servidor confirme la lectura con HTTP 200, para mantener la coherencia del estado offline y la cola de sincronización.

---

## 4. Functional Requirements
- **FR-01**: [Eliminación de Fallback a Notificaciones Leídas] — En `OverviewViewModel`, `observeNotifications` debe asignar a `activeNotification` estrictamente la primera notificación no leída (`status == NotificationStatus.UNREAD && !it.isRead`). Si no existen alertas no leídas, `activeNotification` debe ser `null`. Se debe eliminar cualquier fallback (`?: output.notifications.firstOrNull()`).
- **FR-02**: [Navegación Inmediata y Limpieza Optimista de UI] — Al recibir el evento `OnNotificationPillClicked`, `OverviewViewModel` debe limpiar de forma inmediata `activeNotification = null` y emitir el efecto de navegación correspondiente (`NavigateToProjection` o `NavigateToOnboarding`) sin suspender ni esperar la respuesta remota.
- **FR-03**: [Lectura Asíncrona en Background] — `OverviewViewModel` debe delegar la ejecución de `markNotificationAsReadUseCase` a una corrutina en segundo plano (`Dispatchers.IO`) desacoplada del flujo síncrono de navegación.
- **FR-04**: [Actualización de syncStatus a SYNCED en Room] — En `NotificationRepositoryImpl.markAsRead`, tras recibir una respuesta exitosa (`response.isSuccessful`, HTTP 2xx) de `notificationsApiService.markAsRead(targetId)`, el repositorio debe ejecutar `notificationDao.updateSyncStatus(listOf(targetId), "SYNCED")`.

---

## 5. Acceptance Criteria (Given / When / Then)

### AC-01: Ocultamiento Inmediato y Selección Exclusiva de Notificaciones No Leídas
- **Given** una notificación mostrada en `NotificationPill`,
- **When** el usuario pulsa sobre la píldora o la notificación pasa a estado leído,
- **Then** `activeNotification` se establece en `null` (o en la siguiente notificación no leída si existiera) y la píldora desaparece inmediatamente sin mostrar alertas leídas residuales.

### AC-02: Navegación Inmediata Asíncrona sin Bloqueo de Red
- **Given** una notificación de proyección o Bluetooth visible en `NotificationPill`,
- **When** el usuario pulsa la píldora despachando `OnNotificationPillClicked`,
- **Then** el efecto de navegación (`NavigateToProjection` o `NavigateToOnboarding`) se emite de forma instantánea sin esperar la respuesta HTTP del servidor, ejecutándose el marcado como leído en segundo plano.

### AC-03: Actualización de syncStatus a SYNCED tras HTTP 200
- **Given** una notificación existente en Room con `syncStatus = "PENDING"`,
- **When** el servicio remoto `notificationsApiService.markAsRead(targetId)` responde con éxito (`response.isSuccessful`),
- **Then** `NotificationRepositoryImpl` actualiza en Room el `syncStatus` a `"SYNCED"` para ese `targetId`.

---

## 6. Non-Functional Requirements (Android Constraints)
- **Min SDK**: 24 (Android 7.0 Nougat).
- **Target SDK**: 35 (Android 15).
- **Offline Capability**: **REQUIRED**. La UI responde de inmediato de forma offline-first y la sincronización con Room ocurre de manera atómica.
- **Performance Budget**:
  - Tiempo de despacho del click y emisión del efecto de navegación < 16ms (60 fps sin drops).
  - Cero llamadas de red bloqueantes en el hilo principal (`Dispatchers.Main`).
- **Accessibility Standards**:
  - `NotificationPill` preserva touch target accesible $\ge 48\times 48$dp mientras esté visible.

---

## 7. Out of Scope
- No se modifican los contratos de datos de la API Supabase ni los modelos de dominio existentes.
- No se altera la lógica interna de cálculo de proyecciones de kilometraje ni los umbrales de alerta.
