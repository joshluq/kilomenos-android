# Living Specification: KILOMENOS-5
# Delta Specification: Añadir EntryPoint hacia Destination.NotificationsList en ProfileScreen y Unificación de Notificaciones Internas

**Domain**: `KILOMENOS-5`  
**Schema Standard**: OpenSpec Delta Specification v1.0  
**Gating**: 100% Traceability to Automated Test Cases  

---

## Added & Modified Requirements

### REQ-KILOMENOS-5-001: EntryPoint de Notificaciones en ProfileScreen (AC-01)
- **Given** un usuario autenticado visualizando `ProfileScreen`.
- **When** se renderiza la lista de opciones de configuración (`SettingsItem`).
- **Then** se muestra un elemento interactivo con el texto "Notificaciones", el icono `Icons.Default.Notifications` y un touch target mínimo de 48dp.
- **When** el usuario pulsa sobre la opción "Notificaciones".
- **Then** el `ProfileViewModel` emite el efecto `Effect.NavigateToNotificationsList` y la aplicación navega fluidamente a `Destination.NotificationsList`.

---

### REQ-KILOMENOS-5-002: Reemplazo y Deprecación de StatusCapsule en OverviewScreen (AC-02)
- **Given** la pantalla `OverviewScreen` y su arquitectura visual de 4 capas.
- **When** se compone la interfaz.
- **Then** el componente `StatusCapsule` queda eliminado de la jerarquía visual de `OverviewScreen`.
- **And** cualquier alerta o aviso operacional activo se renderiza única y exclusivamente a través del componente unificado `NotificationPill`.

---

### REQ-KILOMENOS-5-003: Orquestación de Notificaciones de Proyección por Transición de Estado (AC-03)
- **Given** el cálculo de proyecciones de kilometraje del contrato activo (`TripProjection`).
- **When** la proyección cambia de estado seguro (`isOverLimit == false`, Verde) a estado de riesgo de exceso (`isOverLimit == true`, Rojo).
- **Then** el sistema genera y persiste en `NotificationRepository` una notificación con:
  - `topic = NotificationTopic.PROJECTION`
  - `priority = NotificationPriority.CRITICAL`
  - `title = "Alerta de exceso proyectado"`
  - `body = "Tu ritmo actual proyecta superar el límite contratado en X km."`
  - `deepLinkUri = "kmsafe://feature/projection"`
  - `status = NotificationStatus.UNREAD`
- **When** la proyección se mantiene en el mismo estado en recálculos subsiguientes dentro del mismo ciclo.
- **Then** el sistema NO genera notificaciones duplicadas.
- **When** la proyección regresa de estado de exceso a estado seguro (`isOverLimit` cambia a `false`).
- **Then** el sistema genera una notificación informativa (`NotificationPriority.INFO`) indicando la normalización del ritmo.

---

### REQ-KILOMENOS-5-004: Alerta de Configuración Bluetooth para Auto-Tracking (AC-04)
- **Given** un usuario con nivel de suscripción `PREMIUM` y auto-tracking habilitado (`isAutoTrackingEnabled == true`).
- **When** el vehículo activo no tiene configurado ningún dispositivo Bluetooth (`renting?.bluetoothDeviceAddress == null`).
- **Then** el sistema persiste en `NotificationRepository` una notificación con:
  - `topic = NotificationTopic.SYSTEM`
  - `priority = NotificationPriority.WARNING`
  - `title = "Dispositivo Bluetooth no configurado"`
  - `body = "Configura el Bluetooth de tu vehículo para habilitar el auto-tracking inteligente."`
  - `status = NotificationStatus.UNREAD`
- **When** el usuario vincula posteriormente un dispositivo Bluetooth al vehículo.
- **Then** la notificación se resuelve o archiva automáticamente.

---

### REQ-KILOMENOS-5-005: Ciclo de Vida y Persistencia en Historial de Notificaciones (AC-05)
- **Given** una notificación interna generada (proyección, bluetooth o flota) visible en `NotificationPill` de `OverviewScreen`.
- **When** el usuario pulsa sobre la píldora de notificación o la marca como leída desde `NotificationsListScreen`.
- **Then** su estado en `NotificationRepository` se actualiza a `NotificationStatus.READ`.
- **And** la píldora en `OverviewScreen` se oculta automáticamente.
- **And** la notificación permanece almacenada en la base de datos Room y se visualiza en el listado de `NotificationsListScreen` en el estado "Leída".

---

### REQ-KILOMENOS-5-006: Deprecación del Flag `showProjectionBanner` en Preferencias (AC-06)
- **Given** la pantalla `PreferencesScreen` y la entidad de dominio `UserPreferences`.
- **When** se actualiza la configuración de notificaciones.
- **Then** el switch/toggle `showProjectionBanner` queda marcado como obsoleto/deprecado.
- **And** la visibilidad de alertas de proyección en `OverviewScreen` pasa a estar gobernada exclusivamente por la presencia de notificaciones no leídas (`status == UNREAD`) en `NotificationRepository`.

---

### REQ-KILOMENOS-5-007: Resiliencia Offline de Alertas Internas (AC-07)
- **Given** un dispositivo móvil en modo avión o sin conectividad de red celular.
- **When** se actualiza el kilometraje y se dispara una alerta de proyección o de hardware.
- **Then** la alerta se persiste localmente en SQLite Room sin emitir excepciones de red.
- **And** la `NotificationPill` se renderiza de inmediato en `OverviewScreen`.
