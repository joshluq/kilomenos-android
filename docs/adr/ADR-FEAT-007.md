# ADR-FEAT-007: Navegación Contextual e Inmediata en Lista de Notificaciones

**Feature ID**: FEAT-007  
**Jira Issue**: KILOMENOS-8  
**Status**: ACCEPTED  
**Deciders**: Software Architect  
**Date**: 2026-09-24  
**Technical Stakeholders**: Senior Android Developer, QA/Testing Engineer, Product Owner  

---

## 1. Context and Problem Statement
En `NotificationsListScreen`, al pulsar un elemento de la lista de avisos, el evento `NotificationClicked` invocaba `handleNotificationClick(notification)` en `NotificationsListViewModel`. La implementación previa evaluaba si existía un deep link y, en caso contrario, emitía `NotificationsListEffect.NavigateToDetail(notification.id)`.
Esto generaba dos problemas fundamentales:
1. Las notificaciones con topic `PROJECTION` abrían `NotificationDetailScreen`, una pantalla que no ofrece visualizaciones ni herramientas para el análisis del kilometraje proyectado, mostrando además estados en blanco para notificaciones locales. El destino operativo adecuado es `ProjectionAnalysisScreen`.
2. Las alertas de sistema con topic `SYSTEM` vinculadas a la falta de Bluetooth configurado debían dirigir al conductor a la pantalla de edición del contrato (`Destination.Onboarding(vehicleId, isEdit = true)`).
3. No existe suficiente densidad informativa en los avisos para justificar una pantalla de detalle intermedia, por lo que el producto requiere prescindir temporalmente de `NotificationDetailScreen`.
4. La ejecución suspendía esperando la llamada de red a Supabase antes de emitir la navegación, degradando la reactividad.

## 2. Decision Drivers
- **Acción Contextual Directa**: El usuario resuelve la alerta en un solo toque dirigiéndose a la pantalla correspondiente (`ProjectionAnalysisScreen` para proyecciones, edición de contrato para Bluetooth, o Paywall para suscripciones).
- **Optimistic UI con Cero Latencia**: La emisión del efecto de navegación debe ser instantánea (< 16ms) en el hilo principal.
- **Sincronización Asíncrona en Background**: El marcado como leído (`markNotificationAsReadUseCase`) debe correr en `Dispatchers.IO` en segundo plano sin suspender la UI.
- **Arquitectura MVI Limpia**: Nuevos efectos tipados en `NotificationsListEffect` y coordinación transparente en `AppNavigation`.

## 3. Considered Architectural Options
1. **Opción 1: Enriquecer `NotificationDetailScreen` para soportar acciones de proyección y Bluetooth**
   - *Cons*: Introduce complejidad adicional y pantallas intermedias innecesarias para un usuario que busca resolver el aviso de inmediato.
2. **Opción 2: Navegación Contextual Directa desde la Lista y Despacho Asíncrono (Elegida)**
   - *Pros*:
     - Resuelve el problema en 1 click (máxima ergonomía).
     - Evita pantallas en blanco.
     - Preserva la consistencia de navegación con `OverviewViewModel`.
     - 0 latencia perceptible en la interacción.

## 4. Decision Outcome
- **Chosen Option**: Opción 2 — Clean Architecture + MVI con nuevos efectos tipados `NavigateToProjection` y `NavigateToEditContract`.
- **Pattern**: `CLEAN_ARCHITECTURE` con MVI Unidirectional Data Flow.

## 5. System Topology & Implementation Strategy
- **`:feature:notifications`**:
  - `Contract.kt`:
    - `data object NavigateToProjection : NotificationsListEffect`
    - `data class NavigateToEditContract(val vehicleId: String) : NotificationsListEffect`
  - `NotificationsListViewModel.kt`:
    - `handleNotificationClick`: evalúa `notification.topic` y despacha el efecto inmediato antes de lanzar `markNotificationAsReadUseCase` en corrutina background.
  - `NotificationsListRoute.kt`:
    - Añade callbacks `onNavigateToProjection: () -> Unit` y `onNavigateToEditContract: (String) -> Unit`.
- **`:app`**:
  - `AppNavigation.kt`:
    - Conecta `onNavigateToProjection` con `onNavigate(Destination.ProjectionAnalysis)`.
    - Conecta `onNavigateToEditContract` con `onNavigate(Destination.Onboarding(vehicleId, isEdit = true))`.

## 6. Consequences & Tradeoffs
- **Positive**:
  - Navegación contextual instantánea sin pantallas vacías.
  - Comportamiento consistente y alineado entre `NotificationPill` en la cabecera y el buzón completo de notificaciones.
- **Negative**:
  - `NotificationDetailScreen` queda desacoplada de la lista principal (se mantiene en el proyecto para futuras necesidades sin código muerto crítico).
