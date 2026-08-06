package es.joshluq.kmsafe.data.mapper

import es.joshluq.kmsafe.data.remote.response.EntitlementsResponse
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
