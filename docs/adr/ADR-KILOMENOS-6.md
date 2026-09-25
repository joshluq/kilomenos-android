# ADR-KILOMENOS-6: Desacoplamiento de la Cancelación de Notificaciones de Tracking en Compose UI

**Feature ID**: KILOMENOS-6  
**Status**: ACCEPTED  
**Deciders**: Software Architect  
**Date**: 2026-09-25  
**Technical Stakeholders**: Senior Android Developer, QA/Testing Engineer  

---

## 1. Context and Problem Statement
En la arquitectura actual de KmSafe, la cancelación de la notificación del sistema de viaje finalizado (`NOTIFICATION_ID_TRIP_FINISHED`, ID numérico `1002`) se delega indebidamente a la capa de presentación (Compose UI) dentro de `OverviewScreen.kt`. Este comportamiento se dispara mediante un efecto MVI (`Effect.DismissTrackingNotifications`) emitido por `OverviewViewModel.kt`:

```kotlin
Effect.DismissTrackingNotifications -> {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.cancel(1002) // NOTIFICATION_ID_TRIP_FINISHED
}
```

Esta solución infringe múltiples directrices de Clean Architecture y diseño modular:
1. **Fuga de Infraestructura en Compose UI**: La capa de presentación interactúa directamente con servicios de bajo nivel del sistema Android (`NotificationManager`), vulnerando el principio de separación de responsabilidades.
2. **Acoplamiento por Constantes Mágicas**: `OverviewScreen.kt` tiene hardcodeado el identificador `1002`, duplicando la constante privada de `LocationTrackingService` en `:core:tracking`. Si la capa de tracking altera este valor, la UI deja notificaciones huérfanas.
3. **Efectos MVI Impuros**: Los efectos MVI (`UiEffect` / `Effect`) deben representar eventos puntuales de UI (navegación, display de Snackbars, diálogos). Utilizar efectos para ejecutar tareas de ciclo de vida de procesos de fondo añade complejidad innecesaria y entorpece las pruebas unitarias.

---

## 2. Decision Drivers
- **Separación Estricta de Capas**: La capa de presentación en Jetpack Compose solo debe renderizar estado inmutable y despachar eventos de usuario hacia el ViewModel.
- **Modularidad e Inversión de Dependencias**: El ciclo de vida de las notificaciones de tracking pertenece exclusivamente al módulo `:core:tracking` y debe ser orquestado a través de contratos en `:core:domain`.
- **Testabilidad Limpia**: Los ViewModels y casos de uso deben ser verificables en JVM sin requerir mocks del framework de Android (`NotificationManager`, `Context`).
- **Idempotencia y Resiliencia**: La cancelación de la notificación debe completarse de forma segura sin arrojar excepciones si la notificación no existe o no tiene permisos.

---

## 3. Considered Architectural Options

### Opción 1: Envolver `NotificationManager` en un helper inyectado directamente en `OverviewViewModel`
- *Pros*: Elimina el código de `OverviewScreen.kt`.
- *Cons*: Viola la regla arquitectónica de KmSafe ("ViewModels no interactúan directamente con servicios de plataforma o infraestructura; deben consumir UseCases de :core:domain"). Mantiene el acoplamiento con la notificación de tracking en el ViewModel de overview.

### Opción 2: Mantener el `Effect.DismissTrackingNotifications` pero centralizar el ID en una clase común
- *Pros*: Minimiza los cambios de código.
- *Cons*: Mantiene el anti-patrón de manipulación de `NotificationManager` dentro de un Composable en `OverviewScreen.kt`. No resuelve la fuga de infraestructura ni la impureza del canal de efectos.

### Opción 3 (Elegida): Caso de Uso de Dominio (`DismissTripNotificationUseCase`) + Extensión de `TrackingServiceController`
- *Pros*:
  - Cumple 100% con Clean Architecture y las directrices de `AGENTS.md`.
  - El contrato `TrackingServiceController` en `:core:domain` se amplía con `fun dismissTripFinishedNotification()`.
  - `TrackingServiceControllerImpl` en `:core:tracking` gestiona la cancelación de forma nativa e interna usando `LocationTrackingService.NOTIFICATION_ID_TRIP_FINISHED`.
  - Se crea `DismissTripNotificationUseCase` en `:core:domain` para permitir que `OverviewViewModel` invoque la cancelación mediante inyección de dependencias estándar.
  - Se erradica `Effect.DismissTrackingNotifications` y la llamada a `NotificationManager` en `OverviewScreen.kt`.
- *Cons*: Requiere la creación de una nueva interfaz de caso de uso y su binding en Dagger/Hilt.

---

## 4. Decision Outcome
- **Opción Elegida**: Opción 3 — Caso de Uso de Dominio + `TrackingServiceController`.
- **Architecture Pattern**: `CLEAN_ARCHITECTURE` / `MVI`.
- **Justificación**: Proporciona el máximo desacoplamiento, respeta los límites modulares entre `:feature:overview` y `:core:tracking`, y elimina por completo los efectos de infraestructura en Compose UI.

---

## 5. System Topology & Module Structure

### 5.1 `:core:domain` (`es.joshluq.kmsafe.domain`)
- **`TrackingServiceController.kt`**:
  ```kotlin
  interface TrackingServiceController {
      fun startTrackingService()
      fun stopTrackingService()
      fun dismissTripFinishedNotification()
  }
  ```
- **`DismissTripNotificationUseCase.kt`**:
  ```kotlin
  interface DismissTripNotificationUseCase : UseCase<DismissTripNotificationUseCase.Input, DismissTripNotificationUseCase.Output> {
      data object Input : UseCaseInput
      data object Output : UseCaseOutput
  }

  class DismissTripNotificationUseCaseImpl @Inject constructor(
      private val controller: TrackingServiceController
  ) : DismissTripNotificationUseCase {
      override suspend fun invoke(input: DismissTripNotificationUseCase.Input): Result<DismissTripNotificationUseCase.Output> {
          controller.dismissTripFinishedNotification()
          return Result.success(DismissTripNotificationUseCase.Output)
      }
  }
  ```

### 5.2 `:core:tracking` (`es.joshluq.kmsafe.core.tracking`)
- **`LocationTrackingService.kt`**:
  - Visibilidad de `NOTIFICATION_ID_TRIP_FINISHED` actualizada a `internal const val`.
- **`TrackingServiceControllerImpl.kt`**:
  - Implementación de `dismissTripFinishedNotification()`:
    ```kotlin
    override fun dismissTripFinishedNotification() {
        logger.d("TrackingServiceController", "Dismissing trip finished notification")
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(LocationTrackingService.NOTIFICATION_ID_TRIP_FINISHED)
    }
    ```

### 5.3 `:core:infrastructure` (`es.joshluq.kmsafe.infrastructure.di`)
- **`UseCaseModule.kt`**:
  - Binding de Hilt para `DismissTripNotificationUseCase`:
    ```kotlin
    @Binds
    abstract fun bindDismissTripNotificationUseCase(impl: DismissTripNotificationUseCaseImpl): DismissTripNotificationUseCase
    ```

### 5.4 `:feature:overview` (`es.joshluq.kmsafe.feature.overview`)
- **`Contract.kt`**:
  - Eliminación de `data object DismissTrackingNotifications : Effect`.
- **`OverviewScreen.kt`**:
  - Eliminación de la rama `Effect.DismissTrackingNotifications` en el `LaunchedEffect(effects)` y la invocación a `NotificationManager.cancel(1002)`.
- **`OverviewViewModel.kt`**:
  - Inyección de `DismissTripNotificationUseCase`.
  - Ejecución de `dismissTripNotificationUseCase(DismissTripNotificationUseCase.Input)` al confirmar el guardado del odómetro y al cancelar un viaje rastreado.

---

## 6. Consequences & Tradeoffs
- **Positivas**:
  - Compose UI completamente libre de llamadas a servicios de sistema de Android.
  - Cero constantes numéricas mágicas en capas de presentación.
  - Pruebas unitarias de `OverviewViewModel` y `DismissTripNotificationUseCase` 100% deterministas en JVM sin mocks de Android Context.
- **Negativas / Coste**:
  - Creación de un caso de uso adicional en `:core:domain` y su correspondiente binding en Hilt.

---

## 7. Guardrails & Compliance Rules
1. **Prohibición de Frameworks en Presentación**: Ningún Composable ni ViewModel en `:feature:overview` puede importar ni invocar `android.app.NotificationManager`.
2. **Inyección Exclusiva de UseCases**: `OverviewViewModel` solo puede invocar operaciones de tracking a través de `StopTripTrackingUseCase`, `ClearTrackingUseCase` y `DismissTripNotificationUseCase`.
3. **Visibilidad Modular Estricta**: `NOTIFICATION_ID_TRIP_FINISHED` permanece encapsulada en `:core:tracking` con visibilidad `internal`.
