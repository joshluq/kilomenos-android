# Product Requirements Document: Mejoras en el Comportamiento y Lectura de Notificaciones

**Feature ID**: FEAT-005  
**Version**: 1.1.0  
**Status**: APPROVED  
**Author**: Product Owner  
**Date**: 2026-09-24  
**Target Release**: v1.2.0  

---

## 1. Executive Summary & Problem Statement
- **Problem**: En la pantalla principal (`OverviewScreen`), la píldora de notificación (`NotificationPill`) contiene actualmente un botón secundario "Ver más" que satura visualmente el componente y desvía al usuario hacia la lista general de notificaciones en lugar de resolver la acción prioritaria de la alerta. Asimismo, al pulsar una notificación de proyección o de sistema (Bluetooth faltante), la navegación se realizaba de forma genérica mediante deep link o detalle, en vez de dirigir directamente al flujo contextual correspondiente (`ProjectionAnalysisScreen` o `EditContractScreen`) y marcar automáticamente el mensaje como leído. Además, al recalcular periódicamente las proyecciones en el ViewModel, el sistema intentaba republicar alertas con la misma condición aun cuando el usuario ya las había leído y descartado. Por último, persistía en `PreferencesDataSource` la función obsoleta `setShowProjectionBanner`.
- **Value Proposition**: Simplificar `NotificationPill` convirtiéndolo en un área táctil unificada sin botón "Ver más", integrar un icono de campana como `navigationIcon` en `OverviewTopBar` para acceder limpiamente al historial de notificaciones, implementar navegación contextual directa con marcado automático como leído según el tipo de alerta (`PROJECTION` $\rightarrow$ `ProjectionAnalysisScreen`, `SYSTEM` / Bluetooth $\rightarrow$ `EditContractScreen`), introducir el nuevo caso de uso `PublishNotificationIfUnreadUseCase` para validar mediante metadata si la alerta ya fue leída, y eliminar definitivamente las funciones obsoletas de `setShowProjectionBanner` en `PreferencesDataSource` y capas asociadas.

## 2. Target Personas
- **Primary Persona**: Conductor de renting y gestor de flota vehicular que requiere atención inmediata ante desviaciones de kilometraje proyectado o problemas de auto-tracking.
- **User Pain Point**: El usuario pulsa una alerta sobre exceso de kilometraje y tiene que pasar por pantallas intermedias o botones secundarios pequeños ("Ver más"), o experimenta que alertas que ya marcó como leídas vuelven a aparecer en la cabecera tras recalcular la pantalla.
- **Usage Frequency / Environment**: Diaria, interacción rápida en cabecera mientras el vehículo está detenido o al abrir la app.

## 3. User Stories
- **US-01**: Como conductor que visualiza una alerta en `NotificationPill`, quiero pulsar en cualquier parte de la píldora sin botones secundarios confusos, para que la app me lleve directamente a resolver el aviso con un único gesto accesible.
- **US-02**: Como conductor en la pantalla principal, quiero tener un icono de campana de notificaciones en la barra superior (`OverviewTopBar`) como `navigationIcon`, para poder acceder directamente a mi historial de notificaciones sin depender de botones en la píldora.
- **US-03**: Como conductor con una alerta de proyección de kilometraje, quiero pulsar la píldora para ir directamente a la pantalla de análisis de proyección (`ProjectionAnalysisScreen`) y que la notificación se marque como leída automáticamente en el proceso.
- **US-04**: Como conductor con una alerta de Bluetooth no configurado, quiero pulsar la píldora para ir directamente a editar mi vehículo (`EditContractScreen`) y configurar el Bluetooth, marcándose la alerta como leída al instante.
- **US-05**: Como usuario que ya leyó y descartó una alerta de proyección o de Bluetooth del día de hoy, quiero que el sistema no vuelva a mostrarme la misma alerta en la cabecera cuando el ViewModel recalcule los datos en segundo plano.
- **US-06**: Como desarrollador y mantenedor de la arquitectura, quiero eliminar por completo la lógica obsoleta de `setShowProjectionBanner` en `PreferencesDataSource` para que las preferencias permanezcan limpias y sin código muerto.

## 4. Functional Requirements
- **FR-01**: [Simplificación de NotificationPill] — Eliminar el botón "Ver más" dentro de `NotificationPill`, haciendo que todo el contenedor de la píldora sea un único target táctil interactivo con semántica accesible y altura mínima de 48dp.
- **FR-02**: [Acceso a Notificaciones como NavigationIcon] — Añadir un icono de campana de notificaciones en `OverviewTopBar` dentro de `navigationIcon` para navegar a `NotificationsListScreen`.
- **FR-03**: [Navegación Directa y Marcado Automático de Proyecciones] — Al hacer click en `NotificationPill` para una notificación con topic `PROJECTION`, el sistema debe marcarla como leída en el repositorio y emitir el efecto de navegación directa a `ProjectionAnalysisScreen`.
- **FR-04**: [Navegación Directa y Marcado Automático de Bluetooth / Sistema] — Al hacer click en `NotificationPill` para una notificación con topic `SYSTEM` vinculada a Bluetooth faltante, el sistema debe marcarla como leída en el repositorio y navegar directamente a `EditContractScreen` pasando el `vehicleId` del contrato activo.
- **FR-05**: [Nuevo Caso de Uso: PublishNotificationIfUnreadUseCase] — Crear un caso de uso en `:core:domain` que reciba la notificación a publicar y consulte si ya existe en Room una notificación con esa misma clave semántica (`projection_key` / `deduplication_key`). Si existe y ya fue marcada como leída (`isRead == true`), la publicación se omite; si no existe o no ha sido leída, se delega en `PublishNotificationUseCase`.
- **FR-06**: [Integración en OverviewViewModel] — `OverviewViewModel` debe mantener la lógica de cálculo y construcción de la entidad `Notification` con sus metadatos (`projection_key`, `deduplication_key`), pero delegar la publicación en el nuevo caso de uso `PublishNotificationIfUnreadUseCase`.
- **FR-07**: [Eliminación Definitiva de setShowProjectionBanner] — Eliminar por completo la función `setShowProjectionBanner` de `PreferencesDataSource`, `PreferencesRepository`, `PreferencesRepositoryImpl`, `UserPreferences` y `UpdatePreferencesUseCase`.

## 5. Acceptance Criteria (Given / When / Then)

### AC-01: Eliminación del Botón "Ver más" en NotificationPill
- **Given** que existe una notificación activa renderizada en `OverviewScreen`,
- **When** se visualiza el componente `NotificationPill`,
- **Then** el componente no presenta el botón o texto "Ver más" ni icono de flecha secundaria, y toda la píldora responde como un único elemento clicable con altura mínima $\ge 48$dp.

### AC-02: Acceso a Notificaciones como NavigationIcon en OverviewTopBar
- **Given** el usuario en la pantalla principal `OverviewScreen`,
- **When** visualiza la barra superior `OverviewTopBar`,
- **Then** se muestra un icono de campana en el espacio `navigationIcon` que, al ser pulsado, lanza el efecto `NavigateToNotificationsList` abriendo el historial completo.

### AC-03: Navegación Directa y Lectura para Alerta de Proyección
- **Given** una notificación activa con topic `PROJECTION` mostrada en `NotificationPill`,
- **When** el usuario pulsa en la píldora,
- **Then** el sistema ejecuta `markNotificationAsReadUseCase` para marcarla como leída de inmediato (Optimistic UI) y lanza el efecto `NavigateToProjection`, llevando al usuario a `ProjectionAnalysisScreen`.

### AC-04: Navegación Directa y Lectura para Alerta de Bluetooth
- **Given** una notificación activa con topic `SYSTEM` y metadato `deduplication_key = "bt_missing_${contractId}"`,
- **When** el usuario pulsa en la píldora,
- **Then** el sistema marca la notificación como leída y lanza el efecto de navegación `NavigateToOnboarding(vehicleId = contractId, isEdit = true)`, abriendo directamente `EditContractScreen`.

### AC-05: Prevención de Republicación de Notificaciones Ya Leídas (Nuevo Caso de Uso)
- **Given** que una notificación de proyección o Bluetooth para un contrato fue previamente marcada como leída por el usuario,
- **When** `OverviewViewModel` recalcula los datos o evalúa transiciones y envía la notificación al nuevo caso de uso `PublishNotificationIfUnreadUseCase`,
- **Then** el caso de uso detecta que la clave semántica ya fue leída en Room y omite invocar `PublishNotificationUseCase`, impidiendo que la alerta vuelva a aparecer en la cabecera.

### AC-06: Publicación Exitosa de Alertas No Leídas o Nuevas
- **Given** una nueva condición de exceso proyectado o Bluetooth faltante que nunca ha sido notificada o cuya alerta previa no está leída,
- **When** `PublishNotificationIfUnreadUseCase` es invocado por `OverviewViewModel`,
- **Then** el caso de uso verifica la ausencia de un registro previo leído y delega exitosamente en `PublishNotificationUseCase`, persistiendo la alerta en Room y mostrándola en `NotificationPill`.

### AC-07: Eliminación Completa de setShowProjectionBanner
- **Given** el código fuente de preferencias en `:core:domain` e `:core:infrastructure`,
- **When** se compila y verifica el proyecto,
- **Then** `setShowProjectionBanner` y `showProjectionBanner` han sido eliminados de `PreferencesDataSource`, repositorio y casos de uso, sin generar errores de compilación.

---

## 6. Non-Functional Requirements (Android Constraints)
- **Min SDK**: 24 (Android 7.0 Nougat).
- **Target SDK**: 35 (Android 15).
- **Offline Capability**: **REQUIRED**. Toda la verificación de leídos y deduplicación se realiza localmente en SQLite Room.
- **Performance Budget**:
  - Tiempo de despacho de click y transición de pantalla < 16ms (60 fps sin jank).
  - Consulta del nuevo UseCase en Room < 5ms en hilo background (`Dispatchers.IO`).
- **Accessibility Standards**:
  - `NotificationPill` debe tener un target táctil accesible $\ge 48\times 48$dp.
  - El icono de campana en `OverviewTopBar` debe tener descripción TalkBack adecuada (`R.string.acc_notifications`).

---

## 7. Out of Scope
- No se altera la persistencia remota de Supabase ni los contratos de red de KILOMENOS-10 / KILOMENOS-11.
