package es.joshluq.kmsafe.domain.usecase

import es.joshluq.foundationkit.usecase.UseCase
import es.joshluq.foundationkit.usecase.UseCaseInput
import es.joshluq.foundationkit.usecase.UseCaseOutput
import es.joshluq.kmsafe.domain.model.Entitlements
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
        data class Success(val isDowngraded: Boolean) : Output
        data class Ignored(val reason: String) : Output
    }
}

class SyncEntitlementsFromPushUseCaseImpl @Inject constructor(
    private val entitlementsRepository: EntitlementsRepository,
    private val notificationRepository: NotificationRepository
) : SyncEntitlementsFromPushUseCase {

    override suspend fun invoke(input: SyncEntitlementsFromPushUseCase.Input): Result<SyncEntitlementsFromPushUseCase.Output> {
        return runCatching {
            val type = input.payloadData["type"]
            if (type != "ENTITLEMENTS_SYNC" && type != "SUBSCRIPTION_STATUS") {
                return@runCatching SyncEntitlementsFromPushUseCase.Output.Ignored("Unsupported payload type: $type")
            }

            val levelString = input.payloadData["subscription_level"]
            val isDowngrade = input.payloadData["is_downgrade"]?.toBooleanStrictOrNull() ?: false

            val newLevel = when (levelString?.uppercase()) {
                "PREMIUM" -> SubscriptionLevel.PREMIUM
                else -> SubscriptionLevel.FREE
            }

            entitlementsRepository.clearCache()

            if (isDowngrade || newLevel == SubscriptionLevel.FREE) {
                val alert = Notification(
                    id = UUID.randomUUID().toString(),
                    topic = NotificationTopic.SUBSCRIPTION,
                    title = "Tu suscripción ha cambiado a Plan Gratuito",
                    body = "Renueva tu plan Premium para seguir disfrutando de telemetría ilimitada y proyecciones avanzadas.",
                    priority = NotificationPriority.WARNING,
                    status = NotificationStatus.UNREAD,
                    actionLabel = "Renovar Plan",
                    deepLinkUri = "kmsafe://feature/premium"
                )
                notificationRepository.insertOrUpdate(alert)
            }

            SyncEntitlementsFromPushUseCase.Output.Success(isDowngrade)
        }
    }
}
