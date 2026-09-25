# Component Interface Specification: Módulo de Notificaciones y Sincronización de Entitlements

**Feature ID**: FEAT-002 (Jira: KILOMENOS-2)  
**Component Identifiers**: NotificationPill, NotificationsListRoute, NotificationDetailRoute, KmFirebaseMessagingService  
**Packages**:
- `es.joshluq.kmsafe.feature.notifications` (UI & Presentation)
- `es.joshluq.kmsafe.domain.notifications` (`:core:domain`)
- `es.joshluq.kmsafe.infrastructure.notifications` (`:core:infrastructure`)  
**Target Modules**: `:feature:notifications`, `:core:domain`, `:core:infrastructure`, `:core:navigation`, `:core:ui`, `:feature:overview`  
**Architecture Pattern**: Pure MVI + Coordinator/Route Pattern + 4-Layer Clean UI (Staff Compose Standards)  
**Status**: APPROVED  

---

## 1. Presentation Architecture: Route vs. Screen Separation

KmSafe aplica de forma estricta el patrón **Coordinator / Route** separando la infraestructura de Compose y ciclo de vida de la UI visual puramente declarativa y previewable.

### 1.1 NotificationPill (Componente Integrado en OverviewScreen)

Componente monocanal integrado en la parte superior de `OverviewScreen`. Cumple con el objetivo de toque mínimo de 48dp y utiliza exclusivamente tokens de CanvasKit.

```kotlin
package es.joshluq.kmsafe.feature.notifications.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.joshluq.kmsafe.domain.notifications.model.Notification

/**
 * Píldora interactiva monocanal para la sección superior de OverviewScreen.
 * Muestra el topic destacado y el título de la notificación no leída más relevante.
 * Garantiza área táctil >= 48dp y nula lógica de negocio.
 */
@Composable
fun NotificationPill(
    notification: Notification?,
    onPillClick: (Notification) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

---

### 1.2 NotificationsListRoute & NotificationsListScreen (Bandeja Completa)

#### El Coordinador de Navegación (`NotificationsListRoute.kt`)

```kotlin
package es.joshluq.kmsafe.feature.notifications.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID

@Composable
fun NotificationsListRoute(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToDeepLink: (String) -> Unit,
    // Session key para reiniciar filtros y estado temporal en cada apertura
    sessionId: String = rememberSaveable { UUID.randomUUID().toString() },
    viewModel: NotificationsListViewModel = hiltViewModel(key = sessionId)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NotificationsListEffect.NavigateBack -> onNavigateBack()
                is NotificationsListEffect.NavigateToDetail -> onNavigateToDetail(effect.notificationId)
                is NotificationsListEffect.NavigateToDeepLink -> onNavigateToDeepLink(effect.deepLinkUri)
            }
        }
    }

    NotificationsListScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack
    )
}
```

#### La UI Pura y Previewable (`NotificationsListScreen.kt`)

```kotlin
package es.joshluq.kmsafe.feature.notifications.ui.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun NotificationsListScreen(
    uiState: NotificationsListUiState,
    onAction: (NotificationsListUiAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
)
```

---

### 1.3 NotificationDetailRoute & NotificationDetailScreen (Detalle y Auto-Read)

#### El Coordinador de Navegación (`NotificationDetailRoute.kt`)

```kotlin
package es.joshluq.kmsafe.feature.notifications.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun NotificationDetailRoute(
    notificationId: String,
    onNavigateBack: () -> Unit,
    onNavigateToDeepLink: (String) -> Unit,
    // Entity-scoped key para aislar la instancia de ViewModel por notificación
    viewModel: NotificationDetailViewModel = hiltViewModel(
        key = "notification_detail_$notificationId"
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is NotificationDetailEffect.NavigateBack -> onNavigateBack()
                is NotificationDetailEffect.NavigateToDeepLink -> onNavigateToDeepLink(effect.deepLinkUri)
            }
        }
    }

    NotificationDetailScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack
    )
}
```

---

## 2. The 4-Layer Hierarchical Visual Architecture Specification

La pantalla de bandeja `NotificationsListScreen` implementa la jerarquía visual de 4 capas:

| Capa | Nombre | Pregunta Cognitiva | Elementos Concretos en NotificationsListScreen |
| :--- | :--- | :--- | :--- |
| **Layer 1** | **The Pulse / Hero Glanceable** | *¿Cómo está mi estado de alertas?* (< 2s) | Hero Card resumen: badge con conteo de no leídas (`CanvasKitTheme.typography.headingMedium`) y estado de sincronización (FCM conectado). |
| **Layer 2** | **Contextual Decision Radar** | *¿Qué topics requieren atención hoy?* | Carrusel horizontal de Filter Chips: `[ Todos ]`, `[ Suscripción ]`, `[ Proyección ]`, `[ Flota ]` con badges de cantidad. |
| **Layer 3** | **Zero-Friction Action** | *¿Cómo actúo de inmediato?* (< 5s) | Botón de acción rápida: "Marcar todas como leídas" y botón de renovación si existe aviso de downgrade pendiente. |
| **Layer 4** | **Intelligent Diagnostic Feed** | *¿Cuál es el historial completo?* | `LazyColumn` con `items(notifications, key = { it.id })`, agrupadas por fecha (Hoy, Ayer, Esta Semana), con indicador visual leída/no leída. |

---

## 3. UI State & Action Contracts (Unidirectional Data Flow)

### 3.1 Contratos para NotificationsList

```kotlin
package es.joshluq.kmsafe.feature.notifications.ui.list

import androidx.compose.runtime.Immutable
import es.joshluq.kmsafe.domain.notifications.model.Notification
import es.joshluq.kmsafe.domain.notifications.model.NotificationTopic

@Immutable
data class NotificationsListUiState(
    val isLoading: Boolean = false,
    val unreadCount: Int = 0,
    val selectedTopic: NotificationTopic? = null,
    val notifications: List<Notification> = emptyList(),
    val errorMessage: String? = null
)

sealed interface NotificationsListUiAction {
    data object Refresh : NotificationsListUiAction
    data class TopicSelected(val topic: NotificationTopic?) : NotificationsListUiAction
    data class NotificationClicked(val notification: Notification) : NotificationsListUiAction
    data object MarkAllAsReadClicked : NotificationsListUiAction
    data object DismissError : NotificationsListUiAction
}

sealed interface NotificationsListEffect {
    data object NavigateBack : NotificationsListEffect
    data class NavigateToDetail(val notificationId: String) : NotificationsListEffect
    data class NavigateToDeepLink(val deepLinkUri: String) : NotificationsListEffect
}
```

### 3.2 Contratos para NotificationDetail

```kotlin
package es.joshluq.kmsafe.feature.notifications.ui.detail

import androidx.compose.runtime.Immutable
import es.joshluq.kmsafe.domain.notifications.model.Notification

@Immutable
data class NotificationDetailUiState(
    val isLoading: Boolean = false,
    val notification: Notification? = null,
    val errorMessage: String? = null
)

sealed interface NotificationDetailUiAction {
    data object DismissNotification : NotificationDetailUiAction
    data object PrimaryActionClicked : NotificationDetailUiAction
    data object NavigateBackClicked : NotificationDetailUiAction
}

sealed interface NotificationDetailEffect {
    data object NavigateBack : NotificationDetailEffect
    data class NavigateToDeepLink(val deepLinkUri: String) : NotificationDetailEffect
}
```

---

## 4. Domain Layer Specifications (`:core:domain`)

### 4.1 Entidades Inmutables

```kotlin
package es.joshluq.kmsafe.domain.notifications.model

import androidx.compose.runtime.Immutable

enum class NotificationTopic {
    SUBSCRIPTION,
    PROJECTION,
    FLEET,
    SYSTEM
}

enum class NotificationPriority {
    CRITICAL,
    WARNING,
    INFO
}

enum class NotificationStatus {
    UNREAD,
    READ,
    ARCHIVED
}

@Immutable
data class Notification(
    val id: String,
    val topic: NotificationTopic,
    val title: String,
    val body: String,
    val priority: NotificationPriority,
    val status: NotificationStatus,
    val deepLinkUri: String? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
    val actionLabel: String? = null
)
```

### 4.2 Puerto de Repositorio de Notificaciones

```kotlin
package es.joshluq.kmsafe.domain.notifications.repository

import es.joshluq.kmsafe.domain.notifications.model.Notification
import es.joshluq.kmsafe.domain.notifications.model.NotificationTopic
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(topic: NotificationTopic? = null): Flow<List<Notification>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun getNotificationById(id: String): Notification?
    suspend fun markAsRead(id: String)
    suspend fun markAllAsRead()
    suspend fun insertOrUpdate(notification: Notification)
    suspend fun delete(id: String)
}
```

### 4.3 UseCases del Dominio

```kotlin
package es.joshluq.kmsafe.domain.notifications.usecase

import es.joshluq.kmsafe.domain.notifications.model.Notification
import es.joshluq.kmsafe.domain.notifications.model.NotificationTopic
import es.joshluq.kmsafe.domain.notifications.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveActiveNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    operator fun invoke(topic: NotificationTopic? = null): Flow<List<Notification>> =
        repository.observeNotifications(topic)
}

class MarkNotificationAsReadUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(id: String) = repository.markAsRead(id)
}

class PublishNotificationUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(notification: Notification) = repository.insertOrUpdate(notification)
}

/**
 * Caso de uso crítico de Monetización y Sincronización de Entitlements.
 * Disparado ante un FCM push para refrescar entitlements en background/foreground
 * y emitir la alerta correspondiente en la bandeja local.
 */
class SyncEntitlementsFromPushUseCase @Inject constructor(
    private val authRepository: es.joshluq.kmsafe.domain.repository.AuthRepository,
    private val publishNotificationUseCase: PublishNotificationUseCase
) {
    suspend operator fun invoke(payload: Map<String, String>) {
        // 1. Refresca entitlements desde el backend Supabase/Stripe
        val updatedUser = authRepository.refreshCurrentUser()
        
        // 2. Si se detecta downgrade o vencimiento, publica notificación local
        val isDowngraded = payload["is_downgrade"]?.toBoolean() ?: false
        if (isDowngraded) {
            publishNotificationUseCase(
                Notification(
                    id = "downgrade_${System.currentTimeMillis()}",
                    topic = NotificationTopic.SUBSCRIPTION,
                    title = "Suscripción actualizada a Free",
                    body = "Tu plan Premium ha vencido. Renueva ahora para mantener proyecciones avanzadas y sincronización automática.",
                    priority = NotificationPriority.CRITICAL,
                    status = NotificationStatus.UNREAD,
                    deepLinkUri = "kmsafe://feature/premium",
                    actionLabel = "Renovar Plan"
                )
            )
        }
    }
}
```

---

## 5. Staff Engineering Invariants & Guardrails

1. **Desacoplamiento Estricto de Proyecciones**: El módulo `:feature:notifications` no contiene imports de `es.joshluq.kmsafe.feature.projection.*`. Las alertas de proyección son publicadas por `:feature:projection` invocando `PublishNotificationUseCase` con topic `NotificationTopic.PROJECTION` y deep link `kmsafe://feature/projection`.
2. **Exclusión de Odómetro Activo**: El Composable `FloatingTelemetryPill` en `OverviewScreen.kt` no es modificado ni absorbido por `NotificationPill`.
3. **Optimización de Recomposición**:
   - `NotificationPill` sólo se recompone si cambia la instancia inmutable de `Notification`.
   - Listas en `NotificationsListScreen` usan siempre `key = { it.id }` en `LazyColumn`.
   - Touch targets >= 48dp verificados mediante `Modifier.defaultMinSize(minHeight = 48.dp)`.
   - Descripciones accesibles TalkBack en todos los iconos interactivos.
