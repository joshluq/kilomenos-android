package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.Notification
import es.joshluq.kmsafe.domain.model.NotificationPriority
import es.joshluq.kmsafe.domain.model.NotificationStatus
import es.joshluq.kmsafe.domain.model.NotificationTopic
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import es.joshluq.kmsafe.domain.repository.NotificationRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Domain use case to sync entitlements upon receiving a silent or visible push from FCM.
 * Conforms to FoundationKit UseCase architecture.
 */
interface SyncEntitlementsFromPushUseCase :
    UseCase<SyncEntitlementsFromPushUseCase.Input, SyncEntitlementsFromPushUseCase.Output> {

    data class Input(val payloadData: Map<String, String>) : UseCaseInput

    sealed interface Output : UseCaseOutput {
        data class Success(
            val isDowngraded: Boolean,
            val notification: Notification? = null
        ) : Output
        data class Ignored(val reason: String) : Output
    }
}

class SyncEntitlementsFromPushUseCaseImpl @Inject constructor(
    private val entitlementsRepository: EntitlementsRepository,
    private val notificationRepository: NotificationRepository
) : SyncEntitlementsFromPushUseCase {

    companion object {
        private val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    }

    override suspend fun invoke(input: SyncEntitlementsFromPushUseCase.Input): Result<SyncEntitlementsFromPushUseCase.Output> {
        return runCatching {
            val type = input.payloadData["type"]
            val actionCode = input.payloadData["action_code"] ?: input.payloadData["action"]
            val eventType = input.payloadData["event_type"]

            val isEntitlementsEvent = type == "ENTITLEMENTS_SYNC" ||
                type == "SUBSCRIPTION_STATUS" ||
                actionCode == "REFRESH_ENTITLEMENTS" ||
                actionCode == "SYNC_ENTITLEMENTS" ||
                eventType == "SUBSCRIPTION_DOWNGRADED" ||
                input.payloadData.containsKey("subscription_level")

            if (!isEntitlementsEvent) {
                return@runCatching SyncEntitlementsFromPushUseCase.Output.Ignored("Unsupported payload: type=$type, action_code=$actionCode, event_type=$eventType")
            }

            val levelString = input.payloadData["subscription_level"]
            val isDowngrade = input.payloadData["is_downgrade"]?.toBooleanStrictOrNull()
                ?: (eventType == "SUBSCRIPTION_DOWNGRADED" || levelString?.uppercase() == "FREE")

            val newLevel = when (levelString?.uppercase()) {
                "PREMIUM" -> SubscriptionLevel.PREMIUM
                else -> SubscriptionLevel.FREE
            }

            entitlementsRepository.clearCache()

            var alertNotification: Notification? = null

            if (isDowngrade || newLevel == SubscriptionLevel.FREE) {
                entitlementsRepository.downgradeToFree()

                val rawId = input.payloadData["notification_id"] ?: input.payloadData["id"]
                val notifId = rawId?.takeIf { UUID_REGEX.matches(it) } ?: UUID.randomUUID().toString()

                val title = input.payloadData["title"] ?: "Tu plan ha cambiado a Free"
                val body = input.payloadData["message"] ?: input.payloadData["body"]
                    ?: "Tu período Premium ha finalizado. Actualiza tu suscripción para seguir disfrutando de todas las ventajas."
                val deepLink = input.payloadData["deep_link"] ?: input.payloadData["deepLinkUri"] ?: "kmsafe://app/notifications"

                val alert = Notification(
                    id = notifId,
                    topic = NotificationTopic.SUBSCRIPTION,
                    title = title,
                    body = body,
                    priority = NotificationPriority.WARNING,
                    status = NotificationStatus.UNREAD,
                    actionLabel = input.payloadData["action_label"] ?: "Renovar Plan",
                    deepLinkUri = deepLink,
                    origin = input.payloadData["origin"] ?: "REMOTE",
                    syncStatus = "SYNCED",
                    timestampMillis = System.currentTimeMillis(),
                    data = input.payloadData + mapOf("event_type" to (eventType ?: "SUBSCRIPTION_DOWNGRADED"), "sub_type" to "downgrade")
                )
                notificationRepository.insertOrUpdate(alert)
                alertNotification = alert
            }

            SyncEntitlementsFromPushUseCase.Output.Success(
                isDowngraded = isDowngrade,
                notification = alertNotification
            )
        }
    }
}
