package es.joshluq.kmsafe.domain.model

/**
 * Enumeration of features that can be dynamically enabled or disabled
 * based on user entitlements.
 */
enum class Feature(val id: String) {
    /** Automatic trip detection and background tracking. */
    AUTO_TRACKING("auto_tracking"),

    /** AI-driven mileage and budget projections. */
    ADVANCED_PROJECTIONS("advanced_projections"),

    /** Management of more than one active renting contract. */
    MULTI_VEHICLE("multi_vehicle"),

    /** Real-time synchronization with cloud storage. */
    CLOUD_SYNC("cloud_sync");

    companion object {
        fun fromId(id: String): Feature? = entries.find { it.id == id }
    }
}
