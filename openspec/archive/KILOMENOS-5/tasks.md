# Implementation Tasks: KILOMENOS-5

**Feature**: Añadir EntryPoint hacia Destination.NotificationsList en profileScreen y Unificación de Notificaciones Internas  
**Ticket ID**: `KILOMENOS-5`  

---

## Phase 1: Specification & Architecture (Product Owner & Solutions Architect)
- [x] Ingestar ticket Jira KILOMENOS-5 y scaffold de OpenSpec
- [x] Elaborar evaluación de Arquitectura de Soluciones: Generación local offline-first vs. Push backend
- [x] Definir criterios de aceptación estructurados Given/When/Then (AC-01 a AC-07 en `delta_spec.md`)
- [x] Redactar diseño técnico MVI, eliminación de `StatusCapsule` y Tríada de Navegación 3 en `design.md`
- [x] Publicar refinamiento en Jira y solicitar aprobación del usuario (MANDATORY HALT)

## Phase 2: Implementation (Senior Android Developer)
- [x] `:feature:profile`:
  - [x] Añadir `Event.OnNotificationsClicked` y `Effect.NavigateToNotificationsList` a `Contract.kt`
  - [x] Actualizar `ProfileViewModel.kt` para emitir el efecto al pulsar el evento
  - [x] Añadir `SettingsItem` ("Notificaciones") en `ProfileScreen.kt` con tokens CanvasKit y touch target >= 48dp
  - [x] Añadir `onNavigateToNotificationsList` en `ProfileRoute.kt`
- [x] `:app`:
  - [x] Conectar `onNavigateToNotificationsList = { onNavigate(Destination.NotificationsList) }` en `DashboardNavigation.kt` y `AppNavigation.kt`
- [x] `:feature:overview`:
  - [x] Eliminar `StatusCapsule` de la UI en `OverviewScreen.kt`
  - [x] Deprecar/eliminar el modelo `StatusCapsuleUiModel` y sus referencias en `OverviewViewModel.kt`
  - [x] Integrar detección de transición de estado en proyecciones (Verde $\leftrightarrow$ Rojo) y publicar mediante `PublishNotificationUseCase`
  - [x] Integrar alerta de Bluetooth faltante para auto-tracking mediante `PublishNotificationUseCase`
- [x] Unit Tests:
  - [x] Actualizar `ProfileViewModelTest.kt` verificando la emisión de `NavigateToNotificationsList`
  - [x] Actualizar `OverviewViewModelTest.kt` verificando la publicación de notificaciones de proyección ante transiciones y la retirada de `StatusCapsule`
- [x] Verificación de Calidad:
  - [x] Ejecutar `./gradlew :app:assembleDevDebug` verificando 0 errores de compilación
  - [x] Ejecutar `./gradlew :feature:profile:testDebugUnitTest :feature:overview:testDebugUnitTest` verificando 100% de éxito

## Phase 3: Independent Verification & Release (QA / Testing Engineer)
- [x] Matriz de trazabilidad 1 a 1 de todos los criterios de aceptación (AC-01 a AC-07)
- [x] Validación de accesibilidad y semántica en `ProfileScreen` (TalkBack y touch target de 48dp)
- [x] Verificación de ciclo de vida: Al marcar una notificación como leída desaparece de Overview y permanece en historial
- [x] Emisión de veredicto QA (`qa_verdict_KILOMENOS-5.json` y `QA-REPORT-KILOMENOS-5.md`)
- [x] Cierre y transición de ticket en Jira a "Listo"
