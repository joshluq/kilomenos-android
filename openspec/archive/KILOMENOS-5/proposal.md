# Change Proposal: Añadir EntryPoint hacia Destination.NotificationsList en ProfileScreen y Unificación de Notificaciones Internas

**Change ID**: `KILOMENOS-5`  
**Status**: In Review  
**Created At**: 2026-09-22 23:41:14  
**Authors**: Product Owner (`product_owner`) & Solutions Architect (`solutions_architect`)  
**Target Modules**: `:feature:profile`, `:feature:overview`, `:feature:notifications`, `:core:domain`, `:core:infrastructure`, `:app`

---

## 1. Intent & Business Value

### 1.1 Problem Statement
Actualmente existen 3 fricciones críticas en la experiencia de usuario y arquitectura del sistema:
1. **Falta de EntryPoint natural**: El usuario no dispone de un acceso directo y permanente a su historial y bandeja de notificaciones (`Destination.NotificationsList`) desde su perfil (`ProfileScreen`).
2. **Coexistencia duplicada de componentes UI (`StatusCapsule` vs `NotificationPill`)**: En `OverviewScreen` no se completó el reemplazo de `StatusCapsule` por `NotificationPill`. Ambos componentes coexisten visualmente, generando desorden cognitivo (Layer 1 recargado) y duplicidad de alertas.
3. **Alertas internas efímeras y no auditables**: Las alertas generadas internamente por la aplicación (`CriticalRisk` por exceso de km proyectado, `BluetoothMissing` para auto-tracking premium, etc.) se resolvían de forma efímera en memoria en `OverviewViewModel` mediante `StatusCapsuleUiModel`, sin registrarse en la base de datos de notificaciones (`NotificationRepository`). Esto impide que el conductor tenga un registro histórico de cuándo entró en riesgo, qué alertas recibió o qué acciones tomó.
4. **Flag obsoleto en preferencias**: En `PreferencesScreen` persiste el flag `showProjectionBanner` que ocultaba arbitrariamente el banner. Al integrarse en el módulo de notificaciones, una alerta de proyección debe comportarse como cualquier notificación estándar: se notifica ante un **cambio de estado** (de verde a rojo o viceversa), se muestra mientras no se lea, y al marcarse como leída se oculta del inicio pero permanece en la bandeja de notificaciones.

### 1.2 Target Personas
- **Renting Driver / Freelance**: Requiere consultar el historial de avisos de kilometraje y consumo en cualquier momento desde su perfil, sin perder alertas cuando desaparecen de la pantalla principal.
- **Fleet Manager / Conductor Intensivo**: Necesita saber con certeza cuándo un vehículo excedió los límites proyectados o cuándo se configuró la telemetría Bluetooth.

---

## 2. Solutions Architect Evaluation: Local Generation vs. Backend Push

> **Evaluación solicitada en ticket KILOMENOS-5**:  
> *"evaluar si en lugar que el dispositivo muestre directamente la notificación localmente utilizar un endpoint para registrarlo en backend y este inicie el proceso de push y ahí mostrar en la pantalla."*

### 2.1 Trade-off Analysis

| Criterio | Opción A: Solo Push Remoto (Backend Driven) | Opción B: Híbrido Offline-First (Local Store + Backend Sync) [RECOMENDADO] |
|---|---|---|
| **Disponibilidad Offline** | **Crítico (Fallo)**: Si el conductor conduce en zonas sin cobertura (túneles, zonas rurales) y supera el límite de km, la alerta NO se genera ni se muestra. | **100% Garantizada**: La alerta se genera inmediatamente en Room DB local. Funciona sin red. |
| **Latencia de Alerta** | Alta (> 1.5s - 5s): Requiere request HTTP $\to$ Inserción Supabase $\to$ Trigger Edge Function $\to$ Envío FCM $\to$ Recepción en dispositivo. | Inmediata (0ms): Inserción local en SQLite y emisión reactiva a `NotificationPill`. |
| **Consumo de Cuota / Costes** | Genera llamadas a Edge Functions y cuotas FCM por cada recálculo de telemetría u odómetro en el dispositivo. | 0 coste de infraestructura para alertas calculadas en el dispositivo. |
| **Multi-dispositivo** | Notifica a todos los dispositivos vinculados al usuario. | Las alertas locales se sincronizan al backend mediante background worker cuando hay conectividad. |
| **Alertas con App Cerrada** | Muestra notificación en barra del sistema operativo vía FCM `NotificationCompat`. | Se puede lanzar notificación local de sistema (`NotificationManagerCompat`) ante transiciones críticas sin requerir internet. |

### 2.2 Decisión Arquitectónica (ADR-SOL-005)
Se adopta la **Estrategia Híbrida Offline-First**:
1. **Generación Local Inmediata**: Las alertas originadas por cambios de telemetría, proyecciones locales o conectividad Bluetooth se persisten directamente en `NotificationRepository` (SQLite Room) mediante `PublishNotificationUseCase`.
2. **Notificación del Sistema Operativo**: Ante transiciones críticas (ej. paso a estado `CRITICAL` de proyección), la aplicación emite una notificación de sistema local (`NotificationManagerCompat`) con el deep link correspondiente (`kmsafe://feature/projection`).
3. **Canal Remoto FCM Unificado**: Las alertas de servidor (cambio de nivel de suscripción, facturación, avisos de flota globales) se reciben vía FCM en `KmFirebaseMessagingService` e ingresan a `NotificationRepository` exactamente bajo el mismo contrato de dominio.
4. **Desduplicación por Clave Determinista**: Cada alerta de transición de estado genera un `id` determinista (`notif_proj_${contractId}_${transitionEpochDay}` o UUID) para evitar alertas repetidas en el mismo día/estado.

---

## 3. Scope of Changes

### 3.1 Target Modules & Packages
- `:feature:profile`:
  - Añadir opción "Notificaciones" en `ProfileScreen.kt` (con icono `Icons.Default.Notifications` y badge de conteo no leídas opcional).
  - Manejar evento `Event.OnNotificationsClicked` y efecto `Effect.NavigateToNotificationsList` en `ProfileViewModel.kt` y `ProfileRoute.kt`.
  - Deprecar/desactivar el toggle `showProjectionBanner` en `PreferencesScreen.kt` y `PreferencesViewModel.kt`.
- `:feature:overview`:
  - **Eliminar `StatusCapsule`**: Quitar `StatusCapsule.kt`, `StatusCapsuleUiModel.kt` y el bloque `StatusCapsule(...)` de `OverviewScreen.kt`.
  - Mantener exclusivamente `NotificationPill` como el único contenedor de avisos en la cabecera.
  - Implementar detector de transiciones de estado en `OverviewViewModel` para emitir notificaciones a través de `PublishNotificationUseCase` solo cuando haya un cambio real de estado (verde $\leftrightarrow$ rojo, o detección de Bluetooth faltante).
- `:core:domain`:
  - Verificar que `NotificationTopic` cubra: `PROJECTION`, `SUBSCRIPTION`, `FLEET`, `SYSTEM`.
  - Asegurar que `PublishNotificationUseCase` y `ObserveActiveNotificationsUseCase` operen desacoplados con contratos FoundationKit (`UseCase<Input, Output>` / `FlowUseCase<Input, Output>`).
- `:app`:
  - Cablear la navegación de `ProfileRoute` hacia `Destination.NotificationsList` en `AppNavigation.kt`.

---

## 4. Dependencies & Compatibility
- **API Level**: Android SDK 24+ (minSdk), 34 (targetSdk).
- **Design Tokens**: `CanvasKitTheme` estricto en el nuevo `SettingsItem` de `ProfileScreen`.
- **Architecture**: `es.joshluq.foundationkit.viewmodel.ScreenViewModel` y `es.joshluq.foundationkit.usecase.*`.
- **Offline Invariant**: 100% operativo sin conexión a internet.
- **Breaking Changes**: Ninguno. Se mantiene compatibilidad regresiva en base de datos Room v20.
