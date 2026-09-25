# Architecture & Technical Design: Desacoplamiento de la Cancelación de Notificación de Viaje

**Change ID**: `KILOMENOS-6`  
**Architectural Standards**: Clean Architecture, MVI, Compose UI Presentation  

---

## 1. Domain & UseCases ("The Doing")

### 1.1 `TrackingServiceController` (`:core:domain`)
Extensión del contrato de servicio de tracking:
```kotlin
interface TrackingServiceController {
    fun startTrackingService()
    fun stopTrackingService()
    fun dismissTripFinishedNotification()
}
```

### 1.2 `DismissTripNotificationUseCase` (`:core:domain`)
Caso de uso interactuador que delega en el controller:
```kotlin
interface DismissTripNotificationUseCase : UseCase<DismissTripNotificationUseCase.Input, DismissTripNotificationUseCase.Output> {
    data object Input : UseCaseInput
    data object Output : UseCaseOutput
}
```

---

## 2. Infrastructure & Tracking Implementation

### 2.1 `TrackingServiceControllerImpl` (`:core:tracking`)
Interactúa de forma segura con `NotificationManager`:
```kotlin
override fun dismissTripFinishedNotification() {
    logger.d("TrackingServiceController", "Dismissing trip finished notification")
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    notificationManager?.cancel(LocationTrackingService.NOTIFICATION_ID_TRIP_FINISHED)
}
```

### 2.2 `LocationTrackingService` (`:core:tracking`)
`internal const val NOTIFICATION_ID_TRIP_FINISHED = 1002`

---

## 3. Presentation Layer Changes (`:feature:overview`)

- **`Contract.kt`**: Eliminado `Effect.DismissTrackingNotifications`.
- **`OverviewScreen.kt`**: Eliminado el observador de efecto y la llamada a `NotificationManager.cancel(1002)`.
- **`OverviewViewModel.kt`**:
  - Inyecta `DismissTripNotificationUseCase`.
  - Despacha `dismissTripNotificationUseCase` de forma asíncrona en `dispatchers.io` al confirmar el registro de odómetro o al cancelar el viaje rastreado (`handleCancelTrackedTrip`).
