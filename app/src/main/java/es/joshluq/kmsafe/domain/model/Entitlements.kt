package es.joshluq.kmsafe.domain.model

/**
 * Domain model representing the access rights and subscription status of a user.
 */
data class Entitlements(
    val subscriptionLevel: SubscriptionLevel,
    val isTrialActive: Boolean,
    val trialExpiresAt: Long?,
    val enabledFeatures: Set<Feature>,
    val canStartTrial: Boolean,
    val trialFeatures: Set<Feature>
) {
    /**
     * Returns whether a specific feature is currently active and usable.
     */
    fun isFeatureActive(feature: Feature): Boolean {
        return when (subscriptionLevel) {
            SubscriptionLevel.PREMIUM -> true
            SubscriptionLevel.TRIAL -> enabledFeatures.contains(feature)
            SubscriptionLevel.FREE -> false
        }
    }

    /**
     * Returns whether a specific feature can be offered as a trial.
     * A trial is offerable if the user is FREE, eligible for trial, and the feature is in the trial list.
     */
    fun isFeatureTrialable(feature: Feature): Boolean {
        return subscriptionLevel == SubscriptionLevel.FREE && canStartTrial && trialFeatures.contains(feature)
    }

    companion object {
        /** Default entitlements for a new or unauthenticated user. */
        val Default = Entitlements(
            subscriptionLevel = SubscriptionLevel.FREE,
            isTrialActive = false,
            trialExpiresAt = null,
            enabledFeatures = emptySet(),
            canStartTrial = false,
            trialFeatures = emptySet()
        )
    }
}
