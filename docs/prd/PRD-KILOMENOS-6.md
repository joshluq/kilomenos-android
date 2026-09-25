# Product Requirements Document: Desacoplamiento de la Cancelación de Notificación de Viaje

**Feature ID**: KILOMENOS-6  
**Jira Issue**: KILOMENOS-6  
**Version**: 1.0.0  
**Status**: APPROVED  
**Author**: Product Owner  
**Date**: 2026-09-25  
**Target Release**: v1.2.1  

---

## 1. Executive Summary & Problem Statement

### 1.1 Problem Statement
Actualmente, la cancelación de la notificación del sistema de viaje finalizado (`NOTIFICATION_ID_TRIP_FINISHED`, ID 1002) se está gestionando de forma incorrecta a través de la capa de presentación (Compose UI) dentro de `OverviewScreen.kt`, provocada por un efecto MVI (`Effect.DismissTrackingNotifications`) disparado desde `OverviewViewModel.kt`:

```kotlin
Effect.DismissTrackingNotifications -> {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.cancel(1002) // NOTIFICATION_ID_TRIP_FINISHED
}
```

Esta implementación presenta graves anomalías arquitectónicas y de diseño de producto:
1. **Fuga de Infraestructura en la Presentación**: La capa de UI en Jetpack Compose no debe interactuar directamente con servicios del sistema Android (`NotificationManager`) ni gobernar el ciclo de vida de procesos en segundo plano. Su responsabilidad exclusiva es renderizar estado y despachar intenciones de usuario.
2. **Acoplamiento por Constantes Mágicas**: `OverviewScreen.kt` tiene embebido un entero hardcodeado (`1002`) que duplica la constante privada de `LocationTrackingService` en `:core:tracking`. Si la capa de tracking evoluciona o reasigna sus identificadores, `:feature:overview` quedará desincronizada, generando notificaciones huérfanas en la bandeja del sistema.
3. **Violación de Clean Architecture y Modularidad**: El ciclo de vida de los avisos y notificaciones de rastreo vehicular pertenece a la capa de infraestructura/tracking (`:core:tracking`) y debe ser controlado mediante abstracciones y casos de uso en el dominio (`:core:domain`).
4. **Efectos de UI Innecesarios**: El `OverviewViewModel` emite un efecto hacia la UI que no produce ningún cambio visual en pantalla ni navegación, saturando el canal de efectos de MVI con tareas de infraestructura.

### 1.2 Value Proposition
- **Purga de Infraestructura en UI**: Eliminar completamente el efecto `DismissTrackingNotifications` del contrato de `Overview` y la invocación a `NotificationManager` desde `OverviewScreen.kt`.
- **Encapsulación en Capas de Dominio e Infraestructura**: Controlar la cancelación de notificaciones de viaje finalizado directamente a través de contratos de dominio e infraestructura de tracking (`:core:domain` y `:core:tracking`).
- **Experiencia de Usuario Transparente y Limpia**: Garantizar que al guardar un registro de odómetro o al cancelar un viaje rastreado desde la pantalla principal, la notificación del sistema se descarte de forma inmediata, atómica e idempotente sin degradar la fluidez visual (< 16ms).

---

## 2. Target Personas
- **Primary Persona**: Conductor y usuario de KmSafe que finaliza o descarta viajes detectados automáticamente o manuales desde la pantalla principal (`OverviewScreen`).
- **User Pain Point**: Notificaciones persistentes de viajes finalizados que quedan ancladas en la bandeja del sistema si la UI sufre recomposiciones o si ocurren fallos de ciclo de vida al descartar el viaje.
- **Usage Frequency / Environment**: Conducción diaria urbana e interurbana, transiciones frecuentes entre foreground y background, modo offline y conectividad intermitente.

---

## 3. User Stories (INVEST)
- **US-01**: Como conductor que finaliza y guarda un viaje rastreado desde la pantalla de Overview, quiero que la notificación de viaje finalizado se elimine automáticamente de la barra de estado del sistema al confirmar el registro, para mantener mi centro de notificaciones limpio sin esfuerzo manual.
- **US-02**: Como conductor que descarta un viaje no deseado en la tarjeta de viaje de Overview, quiero que la notificación de viaje finalizado se descarte de inmediato sin que la interfaz sufra parpadeos ni bloqueos, para tener una experiencia de usuario ágil y confiable.
- **US-03**: Como equipo de ingeniería y arquitectura, queremos que el ciclo de vida de las notificaciones del servicio de tracking esté 100% encapsulado en las capas de dominio y tracking sin que Compose UI acceda a `NotificationManager` ni a IDs hardcodeados, para respetar Clean Architecture y la modularidad del proyecto.

---

## 4. Functional Requirements
- **FR-01**: [Desacoplamiento de la Capa de Presentación] — La interfaz `OverviewScreen` y el contrato MVI `Contract.kt` de `:feature:overview` no deben contener efectos (`Effect.DismissTrackingNotifications`), callbacks ni referencias directas a `NotificationManager` ni identificadores numéricos mágicos (`1002`).
- **FR-02**: [Encapsulación de Cancelación en Dominio e Infraestructura] — La cancelación de la notificación de viaje finalizado debe ejecutarse a través de abstracciones del dominio (`:core:domain`) implementadas en el módulo de tracking (`:core:tracking`), evitando accesos directos al framework Android desde los ViewModels o vistas de presentación.
- **FR-03**: [Cancelación al Guardar Registro de Odómetro] — Cuando el usuario guarda exitosamente el registro de kilometraje desde la hoja inferior (Bottom Sheet) de Overview tras un viaje rastreado, el sistema debe ejecutar la cancelación de la notificación de viaje finalizado.
- **FR-04**: [Cancelación al Descartar Viaje Rastreado] — Cuando el usuario pulsa en cancelar/descartar el viaje rastreado (`OnCancelTrackedTripClicked`), el sistema debe detener el servicio, limpiar el estado persistido y cancelar la notificación de viaje finalizado en la capa de tracking.
- **FR-05**: [Idempotencia y Tolerancia a Fallos] — La operación de cancelación de la notificación debe ser idempotente: si la notificación ya fue descartada o nunca fue mostrada, la llamada debe completarse de forma segura sin arrojar excepciones ni alterar el flujo de la aplicación.

---

## 5. Acceptance Criteria (Given / When / Then)

### AC-01: Cancelación de Notificación al Guardar Registro de Odómetro
- **Given** un viaje rastreado finalizado pendiente de confirmación en `OverviewScreen` con la notificación de viaje finalizado activa en la bandeja del sistema,
- **When** el usuario introduce el kilometraje y pulsa el botón de guardar registro de odómetro con éxito,
- **Then** el sistema cancela la notificación de viaje finalizado a través del contrato de tracking sin emitir ningún efecto hacia la interfaz Composable.

### AC-02: Cancelación de Notificación al Descartar Viaje Rastreado
- **Given** un viaje rastreado presentado en la tarjeta de viaje en `OverviewScreen` con la notificación de viaje finalizado visible,
- **When** el usuario pulsa en cancelar el viaje rastreado (`OnCancelTrackedTripClicked`),
- **Then** el sistema detiene el tracking, limpia los datos persistidos y cancela la notificación de viaje finalizado directamente desde la lógica de negocio sin intervención de la vista.

### AC-03: Ausencia de Gestión de Notificaciones en Compose UI
- **Given** el módulo `:feature:overview`,
- **When** se analizan las clases `Contract.kt` y `OverviewScreen.kt`,
- **Then** no existe `Effect.DismissTrackingNotifications`, ni llamadas a `context.getSystemService(Context.NOTIFICATION_SERVICE)`, ni referencias a la constante numérico-mágica `1002`.

### AC-04: Idempotencia y Ejecución Segura
- **Given** que la notificación de viaje finalizado ya ha sido descartada por el usuario o no fue emitida por restricciones del sistema operativo,
- **When** se ejecuta la cancelación al guardar o descartar el viaje,
- **Then** la operación finaliza con éxito sin arrojar `SecurityException`, `NullPointerException` ni bloquear la corrutina en ejecución.

---

## 6. Non-Functional Requirements (Android Constraints)
- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 35 (Android 15)
- **Offline Capability**: REQUIRED (operación 100% local en dispositivo sin requerir red)
- **Performance Budget**:
  - Cancelación de notificación despachada en segundo plano con tiempo de ejecución < 16ms.
  - Cero operaciones bloqueantes de I/O o de llamada a servicios de sistema en `Dispatchers.Main`.
- **Accessibility Standards**:
  - Los botones de confirmación y cancelación en `OverviewScreen` mantienen touch targets >= 48x48dp y semántica para TalkBack.
- **Security & Privacy**:
  - Cero datos personales o coordenadas de ubicación persistidas o expuestas en logs durante la cancelación.

---

## 7. Out of Scope
- Modificación del diseño visual, ícono o texto de las notificaciones de tracking.
- Creación de nuevos canales de notificación (`NotificationChannel`) o categorías adicionales.
- Modificación de notificaciones push remotas (FCM) de suscripciones o campañas legales.
