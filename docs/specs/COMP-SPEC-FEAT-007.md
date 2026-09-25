# Component Interface Specification: Navegación Contextual e Inmediata en Lista de Notificaciones

**Feature ID**: FEAT-007  
**Jira Issue**: KILOMENOS-8  
**Component Identifier**: NotificationsListContextualNavigation  
**Package**: es.joshluq.kmsafe.feature.notifications.ui.list  
**Target Modules**: `:feature:notifications`, `:app`  
**Architecture Pattern**: Clean Architecture + UDF MVI  
**Status**: APPROVED  

---

## 1. Contracts & Specifications

### 1.1 `NotificationsListEffect` Contracts (`:feature:notifications`)
Ubicado en `feature/notifications/src/main/java/es/joshluq/kmsafe/feature/notifications/ui/list/Contract.kt`.

```kotlin
sealed interface NotificationsListEffect : UiEffect {
    data object NavigateBack : NotificationsListEffect
    data class NavigateToDetail(val notificationId: String) : NotificationsListEffect
    data class NavigateToDeepLink(val deepLinkUri: String) : NotificationsListEffect
    data object NavigateToPaywall : NotificationsListEffect
    data object NavigateToProjection : NotificationsListEffect
    data class NavigateToEditContract(val vehicleId: String) : NotificationsListEffect
}
```

### 1.2 `NotificationsListViewModel.handleNotificationClick` (`:feature:notifications`)
Ubicado en `feature/notifications/src/main/java/es/joshluq/kmsafe/feature/notifications/ui/list/NotificationsListViewModel.kt`.

```kotlin
    private fun handleNotificationClick(notification: Notification) {
        when (notification.topic) {
            NotificationTopic.PROJECTION -> {
                launchEffect(NotificationsListEffect.NavigateToProjection)
            }
            NotificationTopic.SYSTEM -> {
                val contractId = notification.data?.get("contract_id") as? String ?: ""
                launchEffect(NotificationsListEffect.NavigateToEditContract(contractId))
            }
            NotificationTopic.SUBSCRIPTION -> {
                launchEffect(NotificationsListEffect.NavigateToPaywall)
            }
            else -> {
                val deepLink = notification.deepLinkUri
                if (!deepLink.isNullOrBlank()) {
                    launchEffect(NotificationsListEffect.NavigateToDeepLink(deepLink))
                }
            }
        }

        viewModelScope.launch(dispatcherProvider.io) {
            runCatching {
                markNotificationAsReadUseCase(MarkNotificationAsReadUseCase.Input(notification.id))
            }
        }
    }
```
**Regla de invariante**:
- El efecto de navegación debe lanzarse de manera síncrona/inmediata antes de cualquier corrutina o llamada I/O.
- El marcado como leído se ejecuta en segundo plano (`Dispatchers.IO`) sin suspender la navegación ni lanzar excepciones no controladas.

### 1.3 `NotificationsListRoute` Signature (`:feature:notifications`)
Ubicado en `feature/notifications/src/main/java/es/joshluq/kmsafe/feature/notifications/ui/list/NotificationsListRoute.kt`.

```kotlin
@Composable
fun NotificationsListRoute(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToDeepLink: (String) -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    onNavigateToProjection: () -> Unit = {},
    onNavigateToEditContract: (String) -> Unit = {},
    sessionId: String = rememberSaveable { UUID.randomUUID().toString() },
    viewModel: NotificationsListViewModel = hiltViewModel(key = sessionId)
)
```

### 1.4 `AppNavigation` Integration (`:app`)
Ubicado en `app/src/main/java/es/joshluq/kmsafe/ui/navigation/AppNavigation.kt`.

```kotlin
Destination.NotificationsList -> NavEntry(key) {
    NotificationsListRoute(
        onNavigateBack = onBack,
        onNavigateToDetail = { notificationId ->
            onNavigate(Destination.NotificationDetail(notificationId))
        },
        onNavigateToDeepLink = { uri ->
            when {
                uri.contains("projection") -> onNavigate(Destination.ProjectionAnalysis)
                uri.contains("premium") -> onNavigate(Destination.PremiumPaywall(source = "notification"))
                else -> { /* no-op or external intent */ }
            }
        },
        onNavigateToPaywall = {
            onNavigate(Destination.PremiumPaywall(source = "notification_list"))
        },
        onNavigateToProjection = {
            onNavigate(Destination.ProjectionAnalysis)
        },
        onNavigateToEditContract = { vehicleId ->
            onNavigate(Destination.Onboarding(vehicleId = vehicleId, isEdit = true))
        }
    )
}
```
