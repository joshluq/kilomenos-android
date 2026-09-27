# Component Interface Specification: Integración FCM HTTP v1, Canal de Suscripción y Seguridad en Entitlements

**Feature ID**: KILOMENOS-17  
**Component Identifiers**: NotificationChannelManager / KmFirebaseMessagingService / EntitlementsSecurityInterceptor / NotificationPermissionRationaleDialog  
**Package Hierarchy**: 
- `es.joshluq.kmsafe.infrastructure.notifications`
- `es.joshluq.kmsafe.infrastructure.remote.interceptor`
- `es.joshluq.kmsafe.domain.usecase`
- `es.joshluq.kmsafe.core.ui.components`
**Target Modules**: `:core:infrastructure`, `:core:domain`, `:core:ui`, `:feature:notifications`  
**Architecture Pattern**: Clean Architecture + UDF + MVI  
**Status**: APPROVED  

---

## 1. Infrastructure Layer Contracts (`:core:infrastructure`)

### 1.1 `NotificationChannelManager` Interface & Implementation
Responsable de registrar y verificar los canales de notificación requeridos en Android 8.0+ (API 26+) al inicializarse la aplicación.

```kotlin
package es.joshluq.kmsafe.infrastructure.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationChannelManager {
    fun initializeChannels()
    fun getSubscriptionChannelId(): String
}

@Singleton
class NotificationChannelManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationChannelManager {

    companion object {
        const val CHANNEL_SUBSCRIPTION_ALERTS = "subscription_alerts"
        const val CHANNEL_SUBSCRIPTION_NAME = "Alertas de Suscripción"
        const val CHANNEL_SUBSCRIPTION_DESC = "Avisos sobre cambios en tu estado de suscripción y renovaciones"
    }

    override fun initializeChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createSubscriptionChannel()
        }
    }

    override fun getSubscriptionChannelId(): String = CHANNEL_SUBSCRIPTION_ALERTS

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createSubscriptionChannel() {
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_SUBSCRIPTION_ALERTS,
            CHANNEL_SUBSCRIPTION_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_SUBSCRIPTION_DESC
            enableLights(true)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }
}
```

---

### 1.2 `KmFirebaseMessagingService` Enhancements (Foreground Delivery & Entitlements Refresh)
Actualización del servicio para publicar notificaciones del sistema con `NotificationCompat.Builder` en primer plano cuando se recibe un mensaje con `title` y `body`.

```kotlin
package es.joshluq.kmsafe.infrastructure.notifications

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationPriority
import es.joshluq.kmsafe.domain.model.NotificationStatus
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.domain.usecase.PublishNotificationUseCase
import es.joshluq.kmsafe.domain.usecase.RegisterDeviceTokenUseCase
import es.joshluq.kmsafe.domain.usecase.SyncEntitlementsFromPushUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class KmFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var syncEntitlementsFromPushUseCase: SyncEntitlementsFromPushUseCase

    @Inject
    lateinit var publishNotificationUseCase: PublishNotificationUseCase

    @Inject
    lateinit var registerDeviceTokenUseCase: RegisterDeviceTokenUseCase

    @Inject
    lateinit var channelManager: NotificationChannelManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data

        serviceScope.launch {
            // 1. Reactive Entitlements Invalidation
            if (data.containsKey("subscription_level") ||
                data["action"] == "SYNC_ENTITLEMENTS" ||
                data["action_code"] == "REFRESH_ENTITLEMENTS" ||
                data["event_type"] == "SUBSCRIPTION_DOWNGRADED"
            ) {
                syncEntitlementsFromPushUseCase(SyncEntitlementsFromPushUseCase.Input(data))
            }

            // 2. Visible message payload ingestion
            val notificationPayload = remoteMessage.notification
            if (notificationPayload != null || data.containsKey("title")) {
                val title = notificationPayload?.title ?: data["title"] ?: "KmSafe"
                val body = notificationPayload?.body ?: data["body"] ?: ""
                val deepLink = data["deep_link"] ?: data["deepLinkUri"]

                val notif = Notification(
                    id = data["id"] ?: UUID.randomUUID().toString(),
                    topic = NotificationTopic.SYSTEM,
                    title = title,
                    body = body,
                    priority = NotificationPriority.HIGH,
                    status = NotificationStatus.UNREAD,
                    deepLinkUri = deepLink,
                    timestampMillis = remoteMessage.sentTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
                    actionLabel = data["action_label"],
                    origin = "REMOTE",
                    syncStatus = "SYNCED"
                )
                publishNotificationUseCase(PublishNotificationUseCase.Input(notif))

                // Post system alert with NotificationCompat if channel exists and permissions allow
                postSystemNotification(notif)
            }
        }
    }

    private fun postSystemNotification(notification: Notification) {
        val channelId = channelManager.getSubscriptionChannelId()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(notification.deepLinkUri ?: "kmsafe://notifications")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(this).notify(notification.id.hashCode(), builder.build())
        } catch (_: SecurityException) {
            // Handled when POST_NOTIFICATIONS is not granted
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            registerDeviceTokenUseCase(RegisterDeviceTokenUseCase.Input(token))
        }
    }
}
```

---

### 1.3 `EntitlementsSecurityInterceptor` (Capa 3 de Seguridad Zero-Trust)
Interceptor OkHttp que captura respuestas HTTP 403 con código `PREMIUM_REQUIRED` para ejecutar la degradación inmediata de sesión en la capa de datos.

```kotlin
package es.joshluq.kmsafe.infrastructure.remote.interceptor

import es.joshluq.kmsafe.infrastructure.session.UserSessionDataSource
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntitlementsSecurityInterceptor @Inject constructor(
    private val sessionDataSource: UserSessionDataSource
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 403) {
            val peekedBody = response.peekBody(1024).string()
            val hasPremiumRequired = peekedBody.contains("PREMIUM_REQUIRED") ||
                response.header("X-Error-Code") == "PREMIUM_REQUIRED"

            if (hasPremiumRequired) {
                // Invalidate local session to FREE synchronously or via launch
                runCatching {
                    runBlocking {
                        sessionDataSource.downgradeToFree()
                    }
                }
            }
        }
        return response
    }
}
```

---

## 2. Presentation Layer Contracts (`:core:ui` / `:feature:notifications`)

### 2.1 `NotificationPermissionRationaleDialog` Composable
Diálogo de sensibilización contextual antes de solicitar el permiso del sistema operativo en Android 13+.

```kotlin
package es.joshluq.kmsafe.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun NotificationPermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
)
```

- **Contract States**:
  - `title`: *"Activa tus Alertas de Cobertura"*
  - `description`: *"KmSafe te avisa si tu suscripción cambia o estás cerca de superar el límite de kilómetros de tu póliza para evitar recargos imprevistos."*
  - `confirmButtonText`: *"Continuar y Activar"*
  - `dismissButtonText`: *"Más tarde"*

---

## 3. Data Flow & Testing Verification Contracts

1. **Unit Test: `NotificationChannelManagerTest`**:
   - `given Api26Plus when initializeChannels then creates subscription_alerts channel with IMPORTANCE_HIGH`.
2. **Unit Test: `KmFirebaseMessagingServiceTest`**:
   - `given push with REFRESH_ENTITLEMENTS when onMessageReceived then calls syncEntitlementsFromPush and posts notification`.
3. **Unit Test: `EntitlementsSecurityInterceptorTest`**:
   - `given 403 response with PREMIUM_REQUIRED when intercept then calls downgradeToFree`.
