# ADR-KILOMENOS-18: Diálogo de Confirmación para Eliminación de Notificación con CanvasKitConfirmDialog

**Feature ID**: KILOMENOS-18  
**Status**: ACCEPTED  
**Deciders**: Mobile Software Architect (Android), Senior Android Developer  
**Date**: 2026-09-29  
**Technical Stakeholders**: Product Owner, Senior Android Developer, QA/Testing Engineer  

---

## 1. Context and Problem Statement

En la pantalla de lista de notificaciones (`NotificationsListScreen`) del módulo `:feature:notifications`, cada elemento de notificación dispone de un botón de eliminación (icono de papelera). Actualmente, al pulsar dicho botón, se despacha inmediatamente el evento `DeleteNotificationClicked(notificationId)`, ejecutando de forma destructiva y directa `DeleteNotificationUseCase(input)`.

Esta operación borra de manera inmediata el registro de la base de datos local Room y remota, sin ninguna barrera confirmatoria. En dispositivos móviles, toques accidentales durante la conducción, vibraciones o desplazamientos táctiles provocan la pérdida irreversible de notificaciones críticas sobre límites contractuales de kilometraje, exceso proyectado o alertas de telemetría.

En otros módulos de KmSafe (como `RecordDetailScreen` en `:feature:history`), las operaciones destructivas están protegidas mediante el componente unificado `CanvasKitConfirmDialog` del Design System CanvasKit. Se requiere trasladar este mismo estándar a la lista de notificaciones para homogeneizar la experiencia de usuario y preservar la integridad de los datos.

---

## 2. Decision Drivers

- **Consistencia Visual y de Diseño**: Utilizar el componente estándar `CanvasKitConfirmDialog` de CanvasKit (`isDestructive = true`), respetando los tokens de espaciado, tipografía y accesibilidad de la aplicación.
- **Unidirectional Data Flow (MVI)**: Toda mutación de estado debe residir de forma predecible e inmutable en `NotificationsListState` dentro de `NotificationsListViewModel`. El Composable `NotificationsListScreen` debe permanecer puro y sin estado local (`state hoisting`).
- **Seguridad en la Transacción**: El identificador de la notificación candidata a borrado debe almacenarse en el estado (`notificationPendingDeletion`). La ejecución de `DeleteNotificationUseCase` solo puede desencadenarse si el usuario pulsa deliberadamente la confirmación en el modal.
- **Cancelación Limpia e Idempotente**: Cualquier evento de descarte (botón "Cancelar", toque fuera del diálogo o botón atrás del sistema) debe limpiar el estado sin alterar los datos ni invocar casos de uso.
- **Accesibilidad TalkBack y Touch Targets**: Garantizar que los botones del diálogo y el botón de borrado en la fila cumplan el estándar de touch target $\ge 48\times 48\,\text{dp}$ y dispongan de descripciones semánticas.

---

## 3. Considered Architectural Options

### Opción 1: Estado local de confirmación dentro del Composable (`remember { mutableStateOf(...) }`)
- *Desventajas*: Viola los principios de MVI y arquitectura limpia de KmSafe. Dificulta los tests unitarios del ViewModel, se pierde fácilmente ante recomposiciones complejas y no permite trazabilidad del ciclo de vida del estado.
- *Decisión*: **Descartada**.

### Opción 2: Confirmación mediante AlertDialog básico de Material3
- *Desventajas*: Rompe la coherencia del Design System CanvasKit y diverge del comportamiento y apariencia de `RecordDetailScreen`.
- *Decisión*: **Descartada**.

### Opción 3: Estado modelado en MVI (`NotificationsListState`) con `CanvasKitConfirmDialog` (Elegida)
- *Ventajas*:
  1. `NotificationsListState` almacena `notificationPendingDeletion: NotificationUiItem?` (inmutable).
  2. Nuevas acciones MVI explícitas: `ConfirmDeleteNotificationClicked` y `DismissDeleteNotificationClicked`.
  3. `DeleteNotificationClicked(notificationId)` solo selecciona el ítem pendiente y activa el diálogo.
  4. 100% testeable mediante pruebas unitarias en `NotificationsListViewModelTest` utilizando Turbine.
  5. UI desacoplada que proyecta condicionalmente `CanvasKitConfirmDialog` cuando `notificationPendingDeletion != null`.
- *Decisión*: **Aceptada unánimemente**.

---

## 4. Decision Outcome

- **Opción Elegida**: **Opción 3**
- **Componentes Afectados**:
  - `feature/notifications/src/main/java/es/joshluq/kmsafe/feature/notifications/ui/list/Contract.kt`:
    - Incorporar `notificationPendingDeletion: NotificationUiItem? = null` en `NotificationsListState`.
    - Añadir eventos `ConfirmDeleteNotificationClicked` y `DismissDeleteNotificationClicked` en `NotificationsListEvent`.
  - `feature/notifications/src/main/java/es/joshluq/kmsafe/feature/notifications/ui/list/NotificationsListViewModel.kt`:
    - Actualizar `handleEvent` para interceptar `DeleteNotificationClicked`, buscar el ítem y emitir `notificationPendingDeletion`.
    - Manejar `ConfirmDeleteNotificationClicked` ejecutando `DeleteNotificationUseCase` y limpiando la selección.
    - Manejar `DismissDeleteNotificationClicked` reseteando `notificationPendingDeletion` a `null`.
  - `feature/notifications/src/main/java/es/joshluq/kmsafe/feature/notifications/ui/list/NotificationsListScreen.kt`:
    - Integrar `CanvasKitConfirmDialog` renderizado condicionalmente cuando `uiState.notificationPendingDeletion != null`.
  - Recursos de cadenas (`strings.xml` y `values-en/strings.xml`):
    - Añadir `notifications_delete_confirmation_title`, `notifications_delete_confirmation_message`, `notifications_delete_confirm` y `notifications_delete_cancel`.
  - `NotificationsListViewModelTest.kt`:
    - Pruebas unitarias cubriendo el flujo completo: solicitud de borrado $\rightarrow$ diálogo visible $\rightarrow$ cancelación $\rightarrow$ diálogo cerrado sin borrado; y solicitud $\rightarrow$ confirmación $\rightarrow$ ejecución de UseCase.
