# ADR-KILOMENOS-15: Arquitectura de Arranque Inmediato (Zero-Network Cold Start) y Gestión Reactiva de Downgrade de Suscripción

**Feature ID**: KILOMENOS-15  
**Status**: ACCEPTED  
**Deciders**: Mobile Software Architect  
**Date**: 2026-09-26  
**Technical Stakeholders**: Senior Android Developer, QA/Testing Engineer, Product Owner  

---

## 1. Context and Problem Statement

En la arquitectura previa de KmSafe, la pantalla de inicio (`LaunchScreen`) ejecutaba una secuencia bloqueante de red en `LaunchViewModel.kt` al detectar una sesión activa (`CheckSessionUseCase.Output.ActiveSession`), reteniendo al usuario durante **11.88 segundos** antes de navegar al `Dashboard`:

```kotlin
// LaunchViewModel previo: Anti-patrón de inicio síncrono bloqueante
private fun fetchInitialData() {
    viewModelScope.launch {
        val fingerprint = fingerprintProvider.getFingerprint()
        
        // 1. Fetch Entitlements (Linearized) - ~6.87 segundos con refresh de token
        getEntitlementsUseCase(GetEntitlementsUseCase.Input(fingerprint, forceRefresh = true)).first()
        
        // 2. Fetch Contracts (Deep Sync) - ~4.46 segundos descargando 203 historiales + gastos
        syncContractsUseCase(SyncContractsUseCase.Input).first { it !is SyncContractsUseCase.Output.Progress }
        
        // 3. Artificial delay
        delay(500.milliseconds)
        
        launchEffect(LaunchEffect.NavigateToDashboard)
    }
}
```

Esta implementación genera tres anomalías arquitectónicas críticas:
1. **Violación del Principio Cache-First / Offline-First**: KmSafe ya posee una base de datos local Room que actúa como fuente única de verdad (`Single Source of Truth`). `OverviewViewModel` consume contratos, métricas y agregaciones directamente de Room en < 15ms. Retener la interfaz en una pantalla de carga esperando la red degrada severamente la experiencia de usuario y bloquea el acceso en entornos sin cobertura (garajes subterráneos, zonas rurales).
2. **Duplicidad de Sincronización**: `MainActivity` ya encola `SyncWorker` (WorkManager) para ejecutar en segundo plano la sincronización y migración (`MigrateLocalData`). Ejecutar sincronización síncrona en `LaunchViewModel` compite con WorkManager por el ancho de banda y la base de datos local.
3. **Manejo Desarticulado de Cambios de Suscripción**: La verificación de entitlements no debe bloquear la apertura. Si el usuario fue degradado de `PREMIUM` a `FREE` en el backend, la app debe informar de forma reactiva y no destructiva al usuario sin interrumpir su acceso local.

---

## 2. Decision Drivers

- **Time-to-Interactive (TTI) Inmediato**: Reducir el tiempo de cold start de ~12s a **< 800ms**.
- **Cero Red Bloqueante en Arranque**: Ninguna llamada HTTP/REST o refresh remoto forzado debe ejecutarse en la ruta crítica de navegación de `LaunchScreen`.
- **Desacoplamiento Estricto de Responsabilidades**:
  - `LaunchScreen`: Valida exclusivamente la integridad de la sesión local (`CheckSessionUseCase`) y navega de inmediato.
  - `SyncWorker`: Orquesta la sincronización remota de fondo con restricciones de red (`NetworkType.CONNECTED`).
  - `HistoryScreen` / `ExpensesScreen`: Carga a demanda (on-demand/lazy) y `pull-to-refresh`.
- **Unificación de Notificación ante Downgrade (`PREMIUM -> FREE`)**: Si se detecta un downgrade a `FREE` (ya sea por sincronización asíncrona en background o por push notification FCM en caliente), debe utilizarse exactamente el mismo componente UI unificado: `AppOverlay` (`AppExecutiveHudOverlay`).

---

## 3. Considered Architectural Options

### Opción 1: Paralelizar `getEntitlementsUseCase` y `syncContractsUseCase` en `LaunchViewModel`
- *Pros*: Reduce el tiempo total de 11.88s a ~6.87s (el tiempo de la llamada más lenta).
- *Cons*: Sigue bloqueando la UI durante casi 7 segundos. Falla completamente si el dispositivo está en modo avión o con mala conexión. Incumple el presupuesto de rendimiento de TTI < 800ms.

### Opción 2: Eliminar `syncContractsUseCase` pero mantener `getEntitlementsUseCase(forceRefresh = true)`
- *Pros*: Elimina la descarga de los 203 historiales y gastos en el arranque.
- *Cons*: La llamada de entitlements fuerza una renovación de token que toma ~2.05s más la consulta HTTP (~4.8s). Mantiene un bloqueo inaceptable de casi 7 segundos en el Splash.

### Opción 3 (Elegida): Zero-Network Cold Start + Background WorkManager + Reactividad Unificada de Downgrade
- *Pros*:
  - **TTI < 500ms**: `LaunchViewModel` lee la sesión local desde `TokenRepository`/Tink en ~150ms y despacha inmediatamente `LaunchEffect.NavigateToDashboard`.
  - **100% Offline-First**: Acceso inmediato y funcional al Dashboard con los datos cacheados en Room.
  - **Sincronización Silenciosa y Diferida**: `SyncWorker` realiza la sincronización en background en `Dispatchers.IO` sin frame drops ni bloqueos visuales.
  - **Canal de Downgrade Idéntico**: Cuando la sincronización en background o un evento push FCM detecta la transición `PREMIUM -> FREE`, se emite `AppOverlayState.SubscriptionDowngraded`, consumido por `DashboardViewModel` y presentado en `AppExecutiveHudOverlay`.
- *Cons*: Requiere ampliar `AppOverlayState` con el nuevo estado de degradación y agregar la lógica de detección reactiva en `DashboardViewModel`.

---

## 4. Decision Outcome

- **Opción Elegida**: Opción 3 — Zero-Network Cold Start + Background WorkManager + Canal Reactivo de Downgrade.
- **Patrón Arquitectónico**: `CLEAN_ARCHITECTURE` / `MVI` / `CACHE_FIRST`.
- **Justificación**: Es la única alternativa que satisface el SLO de rendimiento (< 800ms), garantiza la operatividad sin conexión y desacopla la sincronización pesada del ciclo de vida de la presentación.

---

## 5. System Topology & Module Structure

```
 ┌─────────────────────────────────────────────────────────────┐
 │                      Launch Screen                          │
 └──────────────────────────────┬──────────────────────────────┘
                                │ CheckSessionUseCase (Local Token/Tink)
                                ▼
                       ActiveSession (< 150ms)
                                │
                                ▼
 ┌─────────────────────────────────────────────────────────────┐
 │           LaunchEffect.NavigateToDashboard                  │
 └──────────────────────────────┬──────────────────────────────┘
                                │
                                ▼
 ┌─────────────────────────────────────────────────────────────┐
 │                     Dashboard Screen                        │
 │  - Reads Room DB immediately (< 15ms)                       │
 │  - Observes AppOverlayState via ObserveAppOverlayUseCase    │
 └──────────────────────────────┬──────────────────────────────┘
                                │
            ┌───────────────────┴───────────────────┐
            │                                       │
            ▼                                       ▼
 ┌─────────────────────┐                 ┌─────────────────────┐
 │ Background Sync     │                 │ Real-Time FCM Push  │
 │ (WorkManager /      │                 │ (Silent/Active Push)│
 │ Entitlements Sync)  │                 │                     │
 └──────────┬──────────┘                 └──────────┬──────────┘
            │                                       │
            └───────────────────┬───────────────────┘
                                │
                     Detects PREMIUM -> FREE
                                │
                                ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ SetAppOverlayUseCase(AppOverlayState.SubscriptionDowngraded)│
 └──────────────────────────────┬──────────────────────────────┘
                                │
                                ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ AppExecutiveHudOverlay: Displays informative downgrade HUD  │
 │ [Ver Planes] -> Navigates to Paywall                        │
 │ [Entendido]  -> Clears Overlay (AppOverlayState.None)       │
 └─────────────────────────────────────────────────────────────┘
```

### 5.1 `:feature:auth` (`es.joshluq.kmsafe.feature.auth.launch`)
- **`LaunchViewModel.kt`**:
  - Se eliminan las dependencias inyectadas: `syncContractsUseCase`, `getEntitlementsUseCase`, `fingerprintProvider`.
  - Se suprime por completo el método `fetchInitialData()`.
  - Al recibir `CheckSessionUseCase.Output.ActiveSession`, se emite directamente `launchEffect(LaunchEffect.NavigateToDashboard)` sin delays artificiales.

### 5.2 `:core:domain` (`es.joshluq.kmsafe.domain`)
- **`AppOverlayState.kt`**:
  - Se agrega la variante:
    ```kotlin
    data class SubscriptionDowngraded(
        val title: TextProvider = TextProvider.Resource(R.string.overlay_subscription_downgraded_title),
        val message: TextProvider = TextProvider.Resource(R.string.overlay_subscription_downgraded_message)
    ) : AppOverlayState
    ```
- **`ObserveAppOverlayUseCase.kt` & `SetAppOverlayUseCase.kt`**:
  - Permiten orquestar la apertura y cierre del HUD de forma reactiva y centralizada.

### 5.3 `:feature:dashboard` (`es.joshluq.kmsafe.feature.dashboard`)
- **`DashboardViewModel.kt`**:
  - Inyecta `getCurrentUserUseCase` (o `getEntitlementsUseCase`) y `setAppOverlayUseCase`.
  - Registra el `lastSubscriptionLevel`: si el nivel previo era `SubscriptionLevel.PREMIUM` y el nuevo nivel emitido pasa a `SubscriptionLevel.FREE`, ejecuta:
    ```kotlin
    setAppOverlayUseCase(SetAppOverlayUseCase.Input(AppOverlayState.SubscriptionDowngraded()))
        .launchIn(viewModelScope)
    ```
  - Maneja los eventos de dismiss del overlay (`Event.DismissSubscriptionDowngradedOverlay`) y navegación a Paywall (`Event.NavigateToPaywallFromOverlay`).
- **`Contract.kt`**:
  - Agrega eventos de usuario para interactuar con el HUD de downgrade:
    ```kotlin
    sealed interface Event : UiEvent {
        data object OnDismissSubscriptionOverlay : Event
        data object OnUpgradeFromSubscriptionOverlay : Event
    }
    ```

### 5.4 `:core:ui` (`es.joshluq.kmsafe.core.ui.components`)
- **`AppExecutiveHudOverlay.kt`**:
  - Renderiza el contenido contextual para `AppOverlayState.SubscriptionDowngraded`:
    - Tarjeta centrada con estilo CanvasKit.
    - Ícono de suscripción / advertencia estética.
    - Título explicativo y descripción de las características ajustadas.
    - Botón primario: "Ver Planes / Reactivar" (`onUpgrade`).
    - Botón secundario: "Entendido" (`onDismiss`).

---

## 6. Verification and Migration Strategy
- **Unit Tests**:
  - `LaunchViewModelTest`: Verificar que ante `ActiveSession` se emite `NavigateToDashboard` en una sola emisión sin invocar casos de uso de red.
  - `DashboardViewModelTest`: Verificar que ante transición `PREMIUM -> FREE` se despacha `SetAppOverlayUseCase` con `AppOverlayState.SubscriptionDowngraded`.
- **Traceability**: Mapeo directo a los criterios de aceptación `AC-01` a `AC-07` de `PRD-KILOMENOS-15.md`.
