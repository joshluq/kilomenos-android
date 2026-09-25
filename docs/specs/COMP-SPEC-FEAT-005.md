# Component Interface Specification: Interacción Contextual de Alertas y Filtrado Semántico

**Feature ID**: FEAT-005  
**Component Identifier**: NotificationInteractionAndUnreadFilter  
**Package**: es.joshluq.kmsafe  
**Target Modules**: `:core:domain`, `:core:infrastructure`, `:feature:notifications`, `:feature:overview`  
**Architecture Pattern**: Clean Architecture + UDF MVI  
**Status**: APPROVED  

---

## 1. UI Contracts & Component Interfaces

### 1.1 NotificationPill Composable
Ubicado en `feature/notifications/src/main/java/es/joshluq/kmsafe/feature/notifications/components/NotificationPill.kt`:
```kotlin
@Composable
fun NotificationPill(
    notification: Notification?,
    onPillClick: (Notification) -> Unit,
    modifier: Modifier = Modifier
)
```
- Se elimina el parámetro `onViewAllClick: () -> Unit`.
- Se elimina el bloque composable del botón "Ver más".
- Todo el `Row` o `Box` de la píldora recibe `.clickable(onClick = { onPillClick(notification) })` con target $\ge 48$dp.

### 1.2 OverviewTopBar Navigation Icon
Ubicado en `feature/overview/src/main/java/es/joshluq/kmsafe/feature/overview/OverviewScreen.kt`:
```kotlin
@Composable
private fun OverviewTopBar(
    state: State,
    onEvent: (Event) -> Unit
) {
    CanvasKitTopBar(
        title = {
            BrandingLogo(logoSize = 32.dp, isMinimized = false)
        },
        navigationIcon = {
            IconButton(
                onClick = safeClick { onEvent(Event.OnViewAllNotificationsClicked) },
                modifier = Modifier.testTag("overview_notifications_bell_icon")
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = stringResource(CoreR.string.acc_notifications),
                    tint = CanvasKitTheme.colors.textPrimary
                )
            }
        },
        actions = {
            OverviewTopbarActions(state, onEvent)
        },
        centeredTitle = true
    )
}
```

---

## 2. Domain UseCase Contracts

### 2.1 PublishNotificationIfUnreadUseCase
Ubicado en `core/domain/src/main/java/es/joshluq/kmsafe/domain/usecase/PublishNotificationIfUnreadUseCase.kt`:
```kotlin
interface PublishNotificationIfUnreadUseCase :
    UseCase<PublishNotificationIfUnreadUseCase.Input, PublishNotificationIfUnreadUseCase.Output> {

    data class Input(val notification: Notification)

    sealed interface Output {
        data object Published : Output
        data object SkippedAlreadyRead : Output
    }
}
```

### 2.2 PublishNotificationIfUnreadUseCaseImpl
Ubicado en `core/infrastructure/src/main/java/es/joshluq/kmsafe/infrastructure/usecase/PublishNotificationIfUnreadUseCaseImpl.kt`:
```kotlin
@Singleton
class PublishNotificationIfUnreadUseCaseImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val publishNotificationUseCase: PublishNotificationUseCase
) : PublishNotificationIfUnreadUseCase {

    override suspend fun invoke(input: PublishNotificationIfUnreadUseCase.Input): Result<PublishNotificationIfUnreadUseCase.Output> {
        val semanticKey = input.notification.data?.get("projection_key") as? String
            ?: input.notification.data?.get("deduplication_key") as? String

        if (semanticKey != null) {
            val existing = notificationDao.findBySemanticKey(semanticKey)
            if (existing != null && (existing.isRead || existing.status == "READ")) {
                return Result.success(PublishNotificationIfUnreadUseCase.Output.SkippedAlreadyRead)
            }
        }

        publishNotificationUseCase(PublishNotificationUseCase.Input(input.notification))
        return Result.success(PublishNotificationIfUnreadUseCase.Output.Published)
    }
}
```

---

## 3. Presentation Redirection Contract (OverviewViewModel)

En `OverviewViewModel`:
```kotlin
is Event.OnNotificationPillClicked -> {
    viewModelScope.launch {
        markNotificationAsReadUseCase(MarkNotificationAsReadUseCase.Input(event.notification.id))
        when (event.notification.topic) {
            NotificationTopic.PROJECTION -> {
                launchEffect(Effect.NavigateToProjection)
            }
            NotificationTopic.SYSTEM -> {
                val contractId = event.notification.data?.get("contract_id") as? String
                    ?: state.value.renting?.id
                launchEffect(Effect.NavigateToOnboarding(vehicleId = contractId, isEdit = true))
            }
            else -> {
                val deepLink = event.notification.deepLinkUri
                if (!deepLink.isNullOrBlank()) {
                    launchEffect(Effect.NavigateToDeepLink(deepLink))
                } else {
                    launchEffect(Effect.NavigateToNotificationDetail(event.notification.id))
                }
            }
        }
    }
}
```

---

## 4. Elimination of setShowProjectionBanner

Se eliminan completamente:
- `PreferencesRepository.setShowProjectionBanner(userId: String, enabled: Boolean)`
- `PreferencesRepositoryImpl.setShowProjectionBanner(userId: String, enabled: Boolean)`
- `PreferencesDataSource.setShowProjectionBanner(userId: String, enabled: Boolean)`
- `UpdatePreferencesUseCase.Input.showProjectionBanner`
- `UserPreferences.showProjectionBanner`
- `OverviewContract.State.showProjectionBanner`
- Toggle de `showProjectionBanner` en `PreferencesScreen.kt` y `PreferencesViewModel.kt`
