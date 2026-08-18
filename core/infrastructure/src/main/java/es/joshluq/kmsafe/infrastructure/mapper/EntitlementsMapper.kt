package es.joshluq.kmsafe.infrastructure.mapper

import es.joshluq.kmsafe.infrastructure.remote.model.EntitlementsModel
import es.joshluq.kmsafe.infrastructure.remote.response.EntitlementsResponse
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.model.Feature
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Maps [EntitlementsResponse] to domain [Entitlements].
 */
fun EntitlementsResponse.toDomain(): Entitlements {
    val parsedLevel = when (subscriptionLevel?.uppercase()) {
        "FREE" -> SubscriptionLevel.FREE
        "TRIAL" -> SubscriptionLevel.TRIAL
        "PREMIUM" -> SubscriptionLevel.PREMIUM
        else -> SubscriptionLevel.FREE
    }

    val parsedDate = trialExpiresAt?.let { ts ->
        runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(ts)?.time
        }.getOrNull()
    }

    val features = enabledFeatures?.mapNotNull { Feature.fromId(it) }?.toSet() ?: emptySet()
    val trialFeaturesScope = trialFeatures?.mapNotNull { Feature.fromId(it) }?.toSet() ?: emptySet()

    return Entitlements(
        subscriptionLevel = parsedLevel,
        isTrialActive = trialActive ?: false,
        trialExpiresAt = parsedDate,
        enabledFeatures = features,
        canStartTrial = canStartTrial ?: false,
        trialFeatures = trialFeaturesScope
    )
}

/**
 * Maps domain [Entitlements] to data [EntitlementsModel] for session storage.
 */
fun Entitlements.toModel(): EntitlementsModel {
    return EntitlementsModel(
        subscriptionLevel = subscriptionLevel.name,
        isTrialActive = isTrialActive,
        trialExpiresAt = trialExpiresAt,
        enabledFeatures = enabledFeatures.map { it.id },
        canStartTrial = canStartTrial,
        trialFeatures = trialFeatures.map { it.id }
    )
}

/**
 * Maps data [EntitlementsModel] to domain [Entitlements].
 */
fun EntitlementsModel.toDomain(): Entitlements {
    return Entitlements(
        subscriptionLevel = runCatching { SubscriptionLevel.valueOf(subscriptionLevel) }
            .getOrDefault(SubscriptionLevel.FREE),
        isTrialActive = isTrialActive,
        trialExpiresAt = trialExpiresAt,
        enabledFeatures = enabledFeatures.mapNotNull { Feature.fromId(it) }.toSet(),
        canStartTrial = canStartTrial,
        trialFeatures = trialFeatures.mapNotNull { Feature.fromId(it) }.toSet()
    )
}
