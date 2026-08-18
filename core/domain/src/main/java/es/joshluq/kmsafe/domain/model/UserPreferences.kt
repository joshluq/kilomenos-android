package es.joshluq.kmsafe.domain.model

/**
 * Domain model for user-specific application preferences.
 *
 * @property rememberEmail Whether the app should pre-fill the last used email on the login screen.
 * @property lastEmail The email address used in the last successful login.
 */
data class UserPreferences(
    val rememberEmail: Boolean = true,
    val lastEmail: String = "",
    val showProjectionBanner: Boolean = true,
    val lastKnownOverLimit: Boolean? = null,
    val autoTrackingEnabled: Boolean = false,
    val autoTrackingPromotionDismissed: Boolean = false
)
