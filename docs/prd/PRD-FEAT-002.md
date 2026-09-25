# Product Requirements Document: Módulo de Notificaciones y Sincronización de Entitlements

**Feature ID**: FEAT-002 (Jira: KILOMENOS-2)  
**Version**: 1.0.0  
**Status**: APPROVED  
**Author**: Product Owner (Digital Experience & Fintech Growth)  
**Date**: 2026-09-22  
**Target Release**: v1.1.0  

---

## 1. Executive Summary & Problem Statement
- **Problem**: 
  1. **Monetization & Entitlements Sync Gap**: Actualmente la aplicación solo consulta y actualiza los derechos de suscripción (*entitlements*) en el arranque en frío (*cold start*). Cuando un usuario con suscripción activa pasa de **Premium a Free** (por expiración de período, cancelación o fallo de cobro recurrente) y mantiene la app abierta en segundo plano (*background*), la app retiene indebidamente las funcionalidades Premium, generando una brecha operativa y perdiendo la oportunidad de conversión inmediata hacia la reactivación/paywall.
  2. **Ausencia de Canal Unificado de Alertas**: Los usuarios carecen de un mecanismo centralizado para recibir, consultar y revisar alertas remotas (FCM) y advertencias críticas de telemetría o proyecciones.
  3. **Fragmentación Visual en Overview**: `OverviewScreen` requiere una sección superior limpia y unificada en formato píldora para notificaciones sin alterar el principio de responsabilidad única ni competir visualmente con otros controles.
- **Value Proposition**: 
  - Proporcionar un módulo autónomo `:feature:notifications` que gestiona la recepción, persistencia, lectura y bandeja de notificaciones push y locales.
  - Implementar una píldora monocanal interactiva en la parte superior de `OverviewScreen` con soporte de **DeepLink** (las notificaciones de tipo `PROJECTION` navegan directamente a la pantalla de proyecciones, mientras que las generales navegan al detalle).
  - Escuchar mensajes push de Firebase Cloud Messaging (FCM) con orden de refresco para actualizar en tiempo real los *entitlements* en background/foreground, revocando accesos caducados e impulsando la conversión al paywall de renovación.

---

## 2. Target Personas
- **Primary Persona**: Conductor de renting / leasing particular o profesional (Free & Premium).
- **User Pain Point**: Desconoce cuándo su contrato o proyección de kilometraje sufre desviaciones o cuándo ha vencido su suscripción, provocando sorpresas financieras o bloqueos inesperados.
- **Usage Frequency / Environment**: Conducción diaria, app frecuentemente mantenida en segundo plano (*background*), conectividad móvil intermitente.

---

## 3. User Stories
- **US-01**: Como conductor en la pantalla Overview, quiero visualizar la notificación no leída más relevante en una píldora superior compacta (topic y título), para estar informado de eventos críticos sin saturar la interfaz.
- **US-02**: Como usuario, quiero que al pulsar sobre una píldora de notificación se marque como leída automáticamente y me dirija a su pantalla de detalle (para notificaciones generales) o directamente a la pantalla de Proyecciones mediante deep link (para notificaciones de tipo `PROJECTION`), para acceder a la información relevante con cero fricción.
- **US-03**: Como usuario con múltiples notificaciones, quiero pulsar en "Ver más" desde la sección de notificaciones para acceder a un historial cronológico completo de todas mis notificaciones (leídas y no leídas), para consultar avisos pasados a mi propio ritmo.
- **US-04**: Como Product Owner, quiero que un mensaje push de Firebase (FCM) con payload de sincronización actualice inmediatamente los *entitlements* en segundo plano o primer plano cuando el usuario pase de Premium a Free, para revocar funciones de pago en tiempo real y desplegar la notificación con CTA al paywall de reactivación.
- **US-05**: Como desarrollador de módulos de dominio (ej. proyección, renting), quiero emitir alertas a través de un contrato agnóstico en `:core:domain` (con soporte opcional de deep link) sin que `:feature:notifications` conozca la lógica de cálculo ni acople sus dependencias, garantizando el Principio de Responsabilidad Única.

---

## 4. Functional Requirements
- **FR-01**: [Módulo Autónomo] — El sistema dispondrá de un nuevo módulo Gradle `:feature:notifications` registrado en `settings.gradle.kts` con arquitectura limpia (UI, Domain, Data).
- **FR-02**: [Componente Píldora en Overview] — La primera sección de `OverviewScreen` integrará un componente interactivo de píldora que mostrará el topic destacado (ej. `[Suscripción]`, `[Proyección]`, `[Flota]`) y el título del mensaje no leído más reciente.
- **FR-03**: [DeepLink a Proyecciones] — La píldora detectará notificaciones con destino de deep link hacia proyecciones (`kmsafe://feature/projection` o tipo `PROJECTION`) y enrutará directamente a dicha pantalla al pulsar.
- **FR-04**: [Pantalla Detalle con Auto-Mark Read] — Para notificaciones estándar, la navegación se dirigirá a `NotificationDetailScreen` en `:feature:notifications`, marcando automáticamente la notificación como `READ` (leída).
- **FR-05**: [Bandeja de Histórico "Ver más"] — La interfaz ofrecerá una acción "Ver más" que abrirá `NotificationsListScreen`, listando todas las notificaciones paginadas/ordenadas por fecha descendente, con filtrado o distinción leída/no leída.
- **FR-06**: [Recepción FCM y Sincronización de Entitlements] — El servicio `KmFirebaseMessagingService` procesará mensajes push visibles y silenciosos; ante un payload de sincronización de suscripción, invocará la actualización de *entitlements* en local y refrescará el flujo de estado de sesión.
- **FR-07**: [Contrato Agnóstico en Dominio] — En `:core:domain` se definirá el contrato de repositorio o bus de eventos de notificación (`NotificationRepository` / `NotificationBus`) para que cualquier módulo funcional pueda publicar alertas sin acoplamiento con la UI de notificaciones.

---

## 5. Acceptance Criteria (Given / When / Then)
- **AC-01**: [Visualización de Píldora en Overview]
  - **Given** que el usuario tiene al menos una notificación no leída o alerta activa,
  - **When** se renderiza la pantalla `OverviewScreen`,
  - **Then** se muestra una píldora en la sección superior con el tag del topic y el título, cumpliendo un target táctil mínimo de 48x48dp y estilo CanvasKit.
- **AC-02**: [Navegación a Detalle Estándar y Marcado Automático]
  - **Given** que se visualiza una píldora con una notificación general (no proyección),
  - **When** el usuario pulsa sobre la píldora,
  - **Then** la app navega a `NotificationDetailScreen` en `:feature:notifications`, muestra cuerpo completo, fecha y topic, y actualiza el estado a `READ` (leída).
- **AC-03**: [Navegación por DeepLink para Notificaciones de Proyección]
  - **Given** que se visualiza una píldora de notificación de tipo `PROJECTION` o con deeplink a proyecciones,
  - **When** el usuario pulsa sobre la píldora,
  - **Then** la app enruta mediante deeplink directamente a la pantalla de Proyecciones (`kmsafe://feature/projection`), y marca automáticamente la notificación como `READ`.
- **AC-04**: [Listado Completo / Bandeja de Notificaciones]
  - **Given** que el usuario está en `OverviewScreen` con la sección de notificaciones,
  - **When** pulsa la acción "Ver más" o el icono de buzón,
  - **Then** se abre la pantalla `NotificationsListScreen` mostrando el listado ordenado por fecha descendente, con distinción visual entre leídas y no leídas.
- **AC-05**: [Sincronización de Entitlements por FCM Push en Background/Foreground]
  - **Given** que la app se encuentra en primer o segundo plano y el backend registra un cambio de suscripción de Premium a Free,
  - **When** el dispositivo recibe el push de Firebase Messaging con payload de sincronización de entitlements,
  - **Then** el cliente ejecuta `GetEntitlementsUseCase` / `refreshEntitlements()`, persiste el nuevo estado en DataStore, actualiza las compuertas de acceso en la UI en tiempo real y muestra la notificación con CTA de renovación al Paywall.
- **AC-06**: [Desacoplamiento de Alertas de Proyección y Módulos de Dominio]
  - **Given** que el módulo `:feature:projection` detecta una desviación crítica de kilometraje,
  - **When** emite el aviso al contrato de notificaciones del dominio indicando topic `PROJECTION` y destino deep link,
  - **Then** `:feature:notifications` muestra la alerta en la píldora sin importar clases ni dependencias de `:feature:projection`.
- **AC-07**: [Estado Vacío / Colapso de Píldora]
  - **Given** que el usuario no tiene notificaciones no leídas ni alertas activas,
  - **When** se muestra `OverviewScreen`,
  - **Then** la sección de la píldora se colapsa limpiamente sin saltos de layout (CLS = 0) ni espacios en blanco residuales.
- **AC-08**: [Resiliencia Offline]
  - **Given** que el dispositivo no tiene conexión de red,
  - **When** el usuario abre la bandeja o detalle de notificaciones,
  - **Then** se visualizan las notificaciones previamente cacheadas en la base de datos local (Room) sin pantallas de bloqueo ni fallos.

---

## 6. Non-Functional Requirements (Android Constraints)
- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 35 (Latest Android platform release)
- **Offline Capability**: REQUIRED. Las notificaciones se almacenan localmente en Room Database garantizando acceso continuo sin conexión.
- **Performance Budget**:
  - Renderizado de la píldora y transiciones < 16ms por frame (60 fps).
  - Apertura de bandeja de notificaciones < 200ms.
- **Accessibility Standards**:
  - Descripciones de contenido TalkBack en píldora, botones de acción ("Ver más") y celdas de notificación.
  - Tamaño de toque mínimo de 48x48dp en todos los elementos interactivos.
  - Escalado tipográfico fluido hasta 200% sin truncamiento de información crítica.
- **Security & Privacy**:
  - Cero tokens de autenticación o PII sensible en los payloads de notificación FCM.
  - Validación de autenticidad en la deserialización de payloads push.

---

## 7. Telemetry & Growth Analytics Schema
| Evento | Parámetros | Disparador |
|---|---|---|
| `notification_received` | `notification_id`, `topic`, `channel`, `is_silent`, `source` | Push FCM recibido en cliente |
| `notification_pill_clicked` | `notification_id`, `topic`, `title`, `destination_type` | Clic en la píldora de Overview |
| `notification_deeplink_navigated` | `notification_id`, `topic`, `target_uri` | Navegación exitosa por deeplink a proyecciones |
| `notification_opened` | `notification_id`, `topic`, `auto_read` (true), `open_latency_ms` | Apertura de `NotificationDetailScreen` |
| `notifications_list_viewed` | `unread_count`, `total_count` | Apertura de `NotificationsListScreen` |
| `entitlement_sync_triggered` | `trigger_source` ("fcm_push"), `previous_tier`, `new_tier`, `is_downgrade` | Sincronización reactiva de entitlements ejecutada |
| `downgrade_paywall_viewed` | `source` ("notification_downgrade_pill"), `tier_offered` ("PREMIUM") | Acceso al Paywall desde aviso de downgrade |

---

## 8. Out of Scope
- **Active Tracking Pill**: El componente `FloatingTelemetryPill` (modo isla dinámica para grabación activa de trayectos GPS/Bluetooth y odómetro en curso) no forma parte de este módulo y conserva su ciclo de vida y UI independientes.
- **Campañas Push Masivas de Marketing**: Notificaciones broadcast no asociadas al vehículo, a contratos o al ciclo de vida de la suscripción.
