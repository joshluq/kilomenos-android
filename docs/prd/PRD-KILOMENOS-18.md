# Product Requirements Document: Diálogo de Confirmación para Eliminación de Notificaciones

**Feature ID**: KILOMENOS-18  
**Version**: 1.0.0  
**Status**: APPROVED  
**Author**: Product Owner  
**Date**: 2026-09-29  
**Target Release**: v1.4.0  

---

## 1. Executive Summary & Problem Statement

- **Problem**: En la pantalla de lista de notificaciones (`NotificationsListScreen`), pulsar el botón de eliminación (icono de papelera) en cualquier elemento ejecuta un borrado inmediato, destructivo e irreversible a través de `DeleteNotificationUseCase`. En un dispositivo móvil en movimiento, en paradas o con interacción con una sola mano, los toques involuntarios provocan la pérdida accidental de notificaciones críticas de telemetría, proyecciones financieras de renting o recordatorios contractuales, sin ninguna barrera de protección ni posibilidad de deshacer.
- **Value Proposition**: Proteger la experiencia de usuario y prevenir la pérdida accidental de datos mediante la incorporación de un diálogo de confirmación explícito y destructivo utilizando `CanvasKitConfirmDialog` (patrón unificado y validado en otros flujos de la app como `RecordDetailScreen`). El usuario podrá confirmar de forma deliberada la eliminación o cancelar la acción limpiamente en cualquier instante.

---

## 2. Target Personas

- **Primary Persona**: Conductor particular / Usuario de renting de vehículos (Carlos, 34 años).
- **User Pain Point**: Al revisar su bandeja de avisos en una parada, pulsa involuntariamente el botón de papelera al intentar hacer scroll o abrir un aviso sobre riesgo de exceso de kilometraje. La notificación se elimina de inmediato, perdiendo el acceso directo al análisis de proyección.
- **Usage Frequency / Environment**: Diaria o semanal, con interacción táctil móvil y riesgo de falsos toques durante paradas de conducción.

---

## 3. User Stories

- **US-01**: Como conductor, quiero que el sistema me solicite una confirmación explícita antes de eliminar una notificación, para no perder información relevante de mis contratos por toques involuntarios.
- **US-02**: Como conductor, quiero poder cancelar el proceso de borrado pulsando en "Cancelar", tocando fuera del modal o usando el gesto de atrás del dispositivo, para mantener intacta la notificación si cambié de opinión o pulsé por error.
- **US-03**: Como conductor, quiero que al pulsar "Eliminar" en el diálogo de confirmación, la notificación se suprima de inmediato de la lista y el contador de notificaciones no leídas se actualice consecuentemente.

---

## 4. Functional Requirements

- **FR-01: Interceptación de Acción Destructiva**: Al pulsar el icono de papelera en cualquier fila de notificación en `NotificationsListScreen`, el sistema no ejecutará la eliminación inmediata, sino que registrará la notificación objetivo en el estado de la UI y mostrará el diálogo modal `CanvasKitConfirmDialog`.
- **FR-02: Presentación del Diálogo CanvasKitConfirmDialog**: El diálogo debe renderizarse con:
  - Título descriptivo: *"¿Eliminar notificación?"*.
  - Mensaje de advertencia: *"¿Estás seguro de que deseas eliminar esta notificación? Esta acción no se puede deshacer."*.
  - Icono de alerta / papelera: `Icons.Default.DeleteOutline` o `Icons.Default.Delete`.
  - Botón de confirmación destructivo: *"Eliminar"*, con estilo destructivo (`isDestructive = true`).
  - Botón de cancelación: *"Cancelar"*.
- **FR-03: Cancelación y Descarte Limpio**: Si el usuario pulsa "Cancelar", pulsa sobre el scrim exterior o realiza el gesto/botón de retroceso de Android (`onDismissRequest`), el diálogo debe cerrarse inmediatamente, el estado de eliminación pendiente se restablecerá a `null` y la notificación permanecerá en la base de datos local y en la lista visual sin alteraciones.
- **FR-04: Confirmación y Eliminación Local/Remota**: Si el usuario pulsa el botón "Eliminar" en el diálogo, este se cerrará de inmediato y se despachará la ejecución de `DeleteNotificationUseCase(notificationId)`. La notificación desaparecerá reactivamente de la pantalla y el contador de avisos pendientes (`unreadCount`) se actualizará en tiempo real si correspondía a un aviso no leído.
- **FR-05: Internacionalización y Localización Completa**: Todos los literales del diálogo deben estar definidos en `strings.xml` para español (`values/strings.xml`) e inglés (`values-en/strings.xml`).

---

### 4.1 Reglas de Negocio con Consecuencia Explícita si Fallan (BR-xx)

| ID Regla | Enunciado de Regla | Condición de Fallo / Violación | Consecuencia en Sistema / Feedback UI |
|---|---|---|---|
| **BR-01** | Ninguna notificación puede ser eliminada directamente sin confirmación modal activa del usuario. | Disparo de `DeleteNotificationUseCase` directo al pulsar el icono de fila sin confirmación en el diálogo. | Defecto bloqueante en QA. Violación del contrato de seguridad de datos de usuario. |
| **BR-02** | La selección de la notificación a eliminar debe ser idempotente y unívoca por su `id`. | Pérdida de referencia del identificador tras recomposiciones o eventos concurrentes. | Si el ID es nulo o inválido al confirmar, el diálogo se cierra limpiamente sin invocar `DeleteNotificationUseCase` ni alterar otros elementos. |
| **BR-03** | La cancelación del diálogo no debe producir efectos secundarios ni mutar el estado de lectura o visibilidad del elemento. | Cambio de estado no deseado (ej. marcar como leída) al pulsar cancelar. | La notificación mantiene estrictamente sus propiedades previas (`isRead`, `status`, timestamp). |

---

## 5. Acceptance Criteria (Given / When / Then)

### AC-01: Despliegue del Diálogo de Confirmación al Solicitar Borrado
- **Given** un usuario autenticado en la pantalla `NotificationsListScreen` con al menos una notificación visible,
- **When** pulsa sobre el icono de eliminar (papelera) de una notificación específica,
- **Then** el sistema muestra el modal `CanvasKitConfirmDialog` con título *"¿Eliminar notificación?"*, mensaje descriptivo, icono de eliminación y opciones *"Eliminar"* y *"Cancelar"*, permaneciendo la notificación en la lista de fondo sin eliminarse aún.

### AC-02: Cancelación del Diálogo sin Efectos Secundarios
- **Given** el diálogo de confirmación `CanvasKitConfirmDialog` visible en pantalla para una notificación,
- **When** el usuario pulsa el botón *"Cancelar"*, pulsa fuera del área del diálogo o ejecuta el gesto de retroceso del sistema Android,
- **Then** el diálogo se cierra inmediatamente, no se envía ninguna orden de borrado a `DeleteNotificationUseCase` y la notificación permanece inalterada en la lista.

### AC-03: Confirmación Exitosa de la Eliminación
- **Given** el diálogo de confirmación `CanvasKitConfirmDialog` visible para una notificación válida,
- **When** el usuario pulsa el botón destructivo *"Eliminar"*,
- **Then** el diálogo se cierra, el sistema ejecuta `DeleteNotificationUseCase(notificationId)`, la notificación es removida reactivamente de la pantalla y el contador de notificaciones no leídas se decrementa si el aviso estaba sin leer.

### AC-04: Accesibilidad y Touch Targets Conformes
- **Given** el diálogo de confirmación desplegado en pantalla,
- **When** un usuario navega utilizando el lector de pantalla TalkBack,
- **Then** los botones de confirmación y cancelación disponen de etiquetas semánticas claras y touch targets interactivos de al menos 48x48dp.

---

## 6. Non-Functional Requirements (Android Constraints)

- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 35 (Android 15)
- **Offline Capability**: **REQUIRED** (la confirmación y el borrado local operan con normalidad en modo avión o sin cobertura).
- **Performance Budget**:
  - Tiempo de renderizado del diálogo < 16ms (60 fps threshold).
  - Bloqueo en hilo principal: 0ms (`Dispatchers.Main`).
- **Accessibility Standards**:
  - `CanvasKitConfirmDialog` debe asegurar un área táctil mínima de 48x48dp para todos los controles interactivos.
  - Textos descriptivos aptos para servicios de accesibilidad (TalkBack).
- **Design Token Consistency**:
  - Uso exclusivo del componente `CanvasKitConfirmDialog` del Design System CanvasKit, con `isDestructive = true`.

---

## 7. Dudas Abiertas Resueltas (HITL Gate)

| ID Duda | Pregunta / Aspecto Evaluado | Decisión Final Acordada | Estado |
|---|---|---|---|
| **D-01** | ¿Qué componente de confirmación debe utilizarse para mantener consistencia con el diseño de la app? | Utilizar `CanvasKitConfirmDialog` de `es.joshluq.canvaskit.components.feedback.CanvasKitConfirmDialog`, idéntico al implementado en `RecordDetailScreen`. | **RESUELTA** |
| **D-02** | ¿Se requiere permitir la opción de "Deshacer" (Snackbar Undo) tras la eliminación? | No, el diálogo de confirmación previo sustituye la necesidad de Snackbar con Undo y simplifica la lógica de transacciones reactivas. | **RESUELTA** |
| **D-03** | ¿Se contemplan acciones de eliminación masiva en esta entrega? | No, queda explícitamente fuera de alcance; solo eliminación individual por ítem. | **RESUELTA** |

---

## 8. Out of Scope

- Eliminación masiva ("Borrar todas las notificaciones").
- Gesto interactivo de deslizamiento (*Swipe-to-Dismiss*) en las filas de notificación.
- Mecanismo de recuperación / papelera temporal de notificaciones eliminadas.
