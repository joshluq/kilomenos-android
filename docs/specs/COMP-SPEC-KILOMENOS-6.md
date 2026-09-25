# Component Interface Specification: Desacoplamiento de la Cancelación de Notificación de Viaje

**Feature ID**: KILOMENOS-6  
**Component Identifier**: TrackingNotificationLifecycle / OverviewTripCancellation  
**Package**: es.joshluq.kmsafe.feature.overview / es.joshluq.kmsafe.domain / es.joshluq.kmsafe.core.tracking  
**Target Modules**: `:core:domain`, `:core:tracking`, `:core:infrastructure`, `:feature:overview`  
**Architecture Pattern**: Clean Architecture + MVI  
**Status**: APPROVED  

---

## 1. Component Overview & Module Responsibilities

| Module | Component | Responsibility |
| :--- | :--- | :--- |
| `:core:domain` | `TrackingServiceController` | Interfaz de servicio de dominio que define operaciones sobre el ciclo de vida del servicio de tracking, incluida la cancelación de notificaciones. |
| `:core:domain` | `DismissTripNotificationUseCase` | Caso de uso interactuador que encapsula la regla de negocio para descartar la notificación de viaje finalizado. |
| `:core:tracking` | `TrackingServiceControllerImpl` | Implementación concreta que interactúa de forma segura con `NotificationManager` utilizando la constante interna del módulo. |
| `:core:tracking` | `LocationTrackingService` | Servicio en primer plano que contiene la constante `NOTIFICATION_ID_TRIP_FINISHED` con visibilidad `internal`. |
| `:core:infrastructure` | `UseCaseModule` | Configuración de inyección de dependencias (Hilt) para enlazar la implementación del caso de uso. |
| `:feature:overview` | `OverviewContract` | Definición de State, Event y Effect en MVI. Se purga `Effect.DismissTrackingNotifications`. |
| `:feature:overview` | `OverviewViewModel` | Consume `DismissTripNotificationUseCase` al confirmar o descartar el viaje rastreado. |
| `:feature:overview` | `OverviewScreen` | Vista Composable que queda libre de dependencias con `NotificationManager`. |

---

## 2. Public Interfaces & Contract Specifications

### 2.1 Domain Layer (`:core:domain`)

#### `TrackingServiceController.kt`
```kotlin
package es.joshluq.kmsafe.domain.service

/**
 * Domain service interface to control the lifecycle of the location tracking service.
 */
interface TrackingServiceController {
    /**
     * Starts the background/foreground tracking service.
     */
    fun startTrackingService()

    /**
     * Stops the background/foreground tracking service.
     */
    fun stopTrackingService()

    /**
     * Dismisses the trip finished system notification if present.
     */
    fun dismissTripFinishedNotification()
}
```

#### `DismissTripNotificationUseCase.kt`
```kotlin
package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.service.TrackingServiceController
import javax.inject.Inject

/**
 * Domain UseCase to dismiss the trip finished notification.
 */
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

---

### 2.2 Tracking Infrastructure Layer (`:core:tracking`)

#### `LocationTrackingService.kt` (Constante de Notificación)
```kotlin
companion object {
    private const val CHANNEL_ID = "location_tracking_channel_v3"
    private const val NOTIFICATION_ID = 1001
    internal const val NOTIFICATION_ID_TRIP_FINISHED = 1002
    // ...
}
```

#### `TrackingServiceControllerImpl.kt`
```kotlin
package es.joshluq.kmsafe.core.tracking

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.service.TrackingServiceController
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingServiceControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: LoggerKit
) : TrackingServiceController {

    override fun startTrackingService() {
        // ...
    }

    override fun stopTrackingService() {
        // ...
    }

    override fun dismissTripFinishedNotification() {
        logger.d("TrackingServiceController", "Dismissing trip finished notification")
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(LocationTrackingService.NOTIFICATION_ID_TRIP_FINISHED)
    }
}
```

---

### 2.3 Presentation Layer (`:feature:overview`)

#### `Contract.kt`
Se elimina:
```kotlin
// REMOVED: data object DismissTrackingNotifications : Effect
```

#### `OverviewScreen.kt`
Se elimina del bloque `LaunchedEffect(effects)`:
```kotlin
// REMOVED:
// Effect.DismissTrackingNotifications -> {
//     val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//     nm.cancel(1002)
// }
```

#### `OverviewViewModel.kt`
Constructor:
```kotlin
@HiltViewModel
class OverviewViewModel @Inject constructor(
    // ... existing dependencies ...
    private val dismissTripNotificationUseCase: DismissTripNotificationUseCase,
    private val dispatchers: DispatcherProvider
) : BaseViewModel<State, Event, Effect>(...) {
    // ...
}
```

Flujos de llamada:
1. Al guardar registro de odómetro:
```kotlin
clearTrackingUseCase(ClearTrackingUseCase.Input).launchIn(viewModelScope)
viewModelScope.launch(dispatchers.io) {
    dismissTripNotificationUseCase(DismissTripNotificationUseCase.Input)
}
```
2. Al cancelar viaje rastreado (`handleCancelTrackedTrip`):
```kotlin
private fun handleCancelTrackedTrip() {
    analytics.track(KmAnalyticsEvent.Tracking.TrackingCancelled)
    viewModelScope.launch(dispatchers.io) {
        stopTripTrackingUseCase(StopTripTrackingUseCase.Input)
        clearTrackingUseCase(ClearTrackingUseCase.Input).collect()
        dismissTripNotificationUseCase(DismissTripNotificationUseCase.Input)
    }
}
```

---

## 3. Behavioral Invariants & Quality Checklist

1. **Idempotencia**: Llamar a `dismissTripFinishedNotification()` cuando la notificación no está activa no provoca ningún fallo.
2. **Main-Safety**: Todas las operaciones de llamada a casos de uso se despachan a través de `viewModelScope` utilizando `dispatchers.io`.
3. **Cero Dependencias de Framework en UI**: Ni `OverviewScreen` ni `Contract.kt` contienen referencias a `NotificationManager`.
