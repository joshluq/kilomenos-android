package es.joshluq.kmsafe.domain.usecase

import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.User
import javax.inject.Inject

/**
 * Use case to determine if a specific feature is accessible to the user
 * based on their subscription level.
 */
class CheckFeatureAccessUseCase @Inject constructor() {

    /**
     * Executes the access check for a specific [Feature].
     *
     * @param user The current user to check.
     * @param feature The feature being requested.
     * @return True if access is granted, false otherwise.
     */
    operator fun invoke(user: User?, feature: Feature): Boolean {
        if (user == null) return false

        return when (feature) {
            Feature.CLOUD_SYNC -> user.subscriptionLevel == SubscriptionLevel.PREMIUM
            Feature.MULTI_VEHICLE -> user.subscriptionLevel == SubscriptionLevel.PREMIUM
            Feature.ADVANCED_REPORTS -> user.subscriptionLevel == SubscriptionLevel.PREMIUM
        }
    }

    /**
     * Defines gated features within the application.
     */
    enum class Feature {
        /** Real-time backup and multi-device synchronization. */
        CLOUD_SYNC,

        /** Ability to manage more than one vehicle contract. */
        MULTI_VEHICLE,

        /** Professional PDF/CSV reporting and analytics. */
        ADVANCED_REPORTS
    }
}
