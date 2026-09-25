# Product Requirements Document: Navegación Contextual e Inmediata en Lista de Notificaciones

**Feature ID**: FEAT-007  
**Jira Issue**: KILOMENOS-8  
**Version**: 1.0.0  
**Status**: APPROVED  
**Author**: Product Owner  
**Date**: 2026-09-24  
**Target Release**: v1.2.1  

---

## 1. Executive Summary & Problem Statement
- **Problem**:
  En la pantalla de listado de notificaciones (`NotificationsListScreen`), al pulsar sobre una notificación el sistema actualmente ejecuta `handleNotificationClick`, navegando por defecto a `NotificationDetailScreen` (o procesando únicamente `deepLinkUri`).
  Esto genera dos fallos críticos en la experiencia de usuario:
  1. **Navegación a pantalla en blanco / sin valor**: Al pulsar una notificación con topic `PROJECTION`, la app abre `NotificationDetailScreen`, la cual carece de funcionalidad interactiva respecto a los cálculos de kilometraje y, ante identificadores de notificación generados localmente, renderiza una vista vacía o fallida. El usuario necesita acceder directamente a `ProjectionAnalysisScreen`.
  2. **Ausencia de navegación contextual para alertas de Bluetooth**: Las alertas de sistema que avisan de la falta de Bluetooth configurado deben dirigir al usuario directamente a la edición del contrato vehicular (`Destination.Onboarding(vehicleId, isEdit = true)`) para vincular su dispositivo.
  3. **Demora y bloqueo por petición de red**: El marcado como leído espera secuencialmente la respuesta de red antes de disparar el efecto de navegación, generando congelamientos en la UI.
  4. **Omisión de NotificationDetail**: Dado que actualmente no existe información extendida que justifique una pantalla de detalle intermedia, el producto prescinde de ella en este flujo, priorizando la resolución inmediata en un solo toque.

- **Value Proposition**:
  Proporcionar navegación contextual directa, fluida y con latencia cero (Optimistic UI):
  - Las alertas de `PROJECTION` navegan inmediatamente a `ProjectionAnalysisScreen`.
  - Las alertas de `SYSTEM` (Bluetooth faltante) navegan inmediatamente a la edición del contrato vehicular.
  - Las alertas de `SUBSCRIPTION` o con deep link enrutan al Paywall o destino específico.
  - La navegación se dispara en < 16ms en el hilo principal y el marcado como leído se ejecuta de forma asíncrona en segundo plano sin congelar la app.

---

## 2. Target Personas
- **Primary Persona**: Conductor y gestor de flota vehicular que revisa su buzón de notificaciones y requiere resolver alertas con un único toque sin pantallas intermedias innecesarias.
- **User Pain Point**: Pulsar una alerta importante y encontrarse con una pantalla en blanco o tener que pulsar múltiples veces para llegar a la pantalla donde realmente se gestiona el aviso.

---

## 3. User Stories
- **US-01**: Como conductor que revisa su historial de notificaciones, quiero pulsar en una alerta de proyección para ir directamente a `ProjectionAnalysisScreen`, de modo que pueda revisar mis cálculos de kilometraje en un solo toque.
- **US-02**: Como conductor con un aviso de Bluetooth no configurado en la lista, quiero pulsar sobre el aviso para abrir la pantalla de edición del contrato, de modo que pueda configurar mi dispositivo sin rodeos.
- **US-03**: Como usuario de la app, quiero que la transición de pantalla tras pulsar una notificación sea instantánea y que el estado de lectura se sincronice en segundo plano, sin demoras ni bloqueos perceptibles.

---

## 4. Functional Requirements
- **FR-01**: [Navegación Directa a ProjectionAnalysisScreen] — En `NotificationsListViewModel`, al pulsar una notificación con topic `PROJECTION`, se debe emitir un efecto de navegación directo a `ProjectionAnalysisScreen` (`NotificationsListEffect.NavigateToProjection`).
- **FR-02**: [Navegación Directa a Edición de Contrato para Alertas de Bluetooth] — Al pulsar una notificación con topic `SYSTEM` vinculada a Bluetooth (clave `bt_missing`), se debe emitir un efecto de navegación a la edición del vehículo (`NotificationsListEffect.NavigateToEditContract(vehicleId = contractId)`).
- **FR-03**: [Navegación Inmediata Asíncrona (Optimistic UI)] — El click en la notificación debe emitir el efecto de navegación de inmediato sin esperar la respuesta remota del servidor, despachando `markNotificationAsReadUseCase` en segundo plano en `Dispatchers.IO`.
- **FR-04**: [Mapeo en AppNavigation] — En `AppNavigation.kt`, `NotificationsListRoute` debe coordinar los nuevos efectos (`NavigateToProjection`, `NavigateToEditContract`) redirigiendo respectivamente a `Destination.ProjectionAnalysis` y `Destination.Onboarding(vehicleId, isEdit = true)`.
- **FR-05**: [Actualización Reactiva de Lectura] — Al pulsar la notificación, esta debe reflejarse como leída en Room actualizando el estado de la lista y decrementando el contador de no leídos.

---

## 5. Acceptance Criteria (Given / When / Then)

### AC-01: Navegación Directa a ProjectionAnalysisScreen
- **Given** el usuario visualizando la lista de notificaciones en `NotificationsListScreen`,
- **When** pulsa sobre una notificación con topic `PROJECTION`,
- **Then** el sistema emite inmediatamente el efecto de navegación hacia `Destination.ProjectionAnalysis` sin pasar por la pantalla de detalle.

### AC-02: Navegación Directa a Edición de Contrato para Alertas de Bluetooth
- **Given** una notificación en la lista con topic `SYSTEM` vinculada a Bluetooth no configurado (`deduplication_key` con `bt_missing` o presencia de `contract_id`),
- **When** el usuario pulsa sobre la notificación,
- **Then** el sistema obtiene el `contract_id` y emite el efecto de navegación hacia `Destination.Onboarding(vehicleId = contractId, isEdit = true)`.

### AC-03: Navegación Inmediata Asíncrona (Optimistic UI)
- **Given** una notificación pulsada en `NotificationsListScreen`,
- **When** se despacha el evento `NotificationClicked`,
- **Then** el efecto de navegación se emite de forma inmediata en el hilo principal (< 16ms) y el caso de uso `markNotificationAsReadUseCase` se ejecuta en background (`Dispatchers.IO`) sin demorar la transición de pantalla.

### AC-04: Soporte de Alertas de Suscripción y Deep Links
- **Given** una notificación con topic `SUBSCRIPTION` o con `deepLinkUri` configurado,
- **When** el usuario pulsa sobre ella,
- **Then** el sistema navega a `Destination.PremiumPaywall` o procesa el deep link contextual correspondiente, sin abrir `NotificationDetailScreen`.

### AC-05: Coherencia de Estado Local y Contador de No Leídos
- **Given** una notificación no leída en la lista,
- **When** el usuario pulsa sobre ella,
- **Then** la notificación se marca como leída localmente en Room de forma optimista y el contador de notificaciones no leídas se actualiza automáticamente.

---

## 6. Non-Functional Requirements (Android Constraints)
- **Min SDK**: 24 (Android 7.0 Nougat).
- **Target SDK**: 35 (Android 15).
- **Offline Capability**: **REQUIRED**. Toda la navegación y actualización local es offline-first.
- **Performance Budget**: Emisión de efecto y navegación en < 16ms (60 fps sin dropped frames).
- **Accessibility Standards**: Elementos de la lista preservan accesibilidad TalkBack y targets $\ge 48$dp.

---

## 7. Out of Scope
- No se elimina la pantalla `NotificationDetailScreen` del código base, únicamente se desacopla su navegación desde la lista de notificaciones.
