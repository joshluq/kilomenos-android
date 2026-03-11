package es.joshluq.kmsafe.domain.model

/**
 * Defines the available subscription tiers for the user.
 */
enum class SubscriptionLevel {
    /** Basic tier with local-only storage and usage limits. */
    FREE,

    /** Premium tier with cloud synchronization and advanced features. */
    PREMIUM
}
