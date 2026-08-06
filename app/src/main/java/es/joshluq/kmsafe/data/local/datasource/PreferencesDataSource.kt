package es.joshluq.kmsafe.data.local.datasource

import es.joshluq.foundationkit.provider.StorageProvider
import es.joshluq.foundationkit.provider.read
import es.joshluq.foundationkit.provider.save
import es.joshluq.kmsafe.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataSource for persistent user settings using Secure StorageProvider.
 */
@Singleton
class PreferencesDataSource @Inject constructor(
    private val storage: StorageProvider
) {
    companion object {
        private const val KEY_REMEMBER_EMAIL = "global_remember_email"
        private const val KEY_LAST_EMAIL = "global_last_email"
        
        private fun bannerKey(userId: String) = "ui_${userId}_show_projection_banner"
        private fun limitKey(userId: String) = "ui_${userId}_last_known_over_limit"
        private fun autoTrackingKey(userId: String) = "settings_${userId}_auto_tracking_enabled"
        private fun promotionDismissedKey(userId: String) = "ui_${userId}_auto_tracking_promotion_dismissed"
    }

    /**
     * Reads all preferences for a user, combining global and per-user data.
     */
    fun getPreferences(userId: String): Flow<UserPreferences> = flow {
        val rememberEmail = storage.read(KEY_REMEMBER_EMAIL) ?: true
        val lastEmail = storage.read(KEY_LAST_EMAIL) ?: ""
        
        val showBanner = if (userId.isNotEmpty()) {
            storage.read(bannerKey(userId)) ?: true
        } else true
        
        val lastLimit = if (userId.isNotEmpty()) {
            storage.read<Boolean>(limitKey(userId))
        } else null

        val autoTracking = if (userId.isNotEmpty()) {
            storage.read<Boolean>(autoTrackingKey(userId)) ?: false
        } else false

        val promotionDismissed = if (userId.isNotEmpty()) {
            storage.read<Boolean>(promotionDismissedKey(userId)) ?: false
        } else false

        emit(
            UserPreferences(
                rememberEmail = rememberEmail,
                lastEmail = lastEmail,
                showProjectionBanner = showBanner,
                lastKnownOverLimit = lastLimit,
                autoTrackingEnabled = autoTracking,
                autoTrackingPromotionDismissed = promotionDismissed
            )
        )
    }

    /**
     * Reads global preferences only.
     */
    fun getGlobalPreferences(): Flow<UserPreferences> = flow {
        val rememberEmail = storage.read<Boolean>(KEY_REMEMBER_EMAIL) ?:true
        val lastEmail = storage.read<String>(KEY_LAST_EMAIL) ?: ""
        emit(UserPreferences(rememberEmail = rememberEmail, lastEmail = lastEmail))
    }

    suspend fun setRememberEmail(enabled: Boolean) {
        storage.save(KEY_REMEMBER_EMAIL, enabled)
    }

    suspend fun saveLastEmail(email: String) {
        storage.save(KEY_LAST_EMAIL, email)
    }

    suspend fun setShowProjectionBanner(userId: String, enabled: Boolean) {
        if (userId.isEmpty()) return
        storage.save(bannerKey(userId), enabled)
    }

    suspend fun setLastKnownOverLimit(userId: String, overLimit: Boolean?) {
        if (userId.isEmpty()) return
        if (overLimit == null) {
            storage.delete(limitKey(userId))
        } else {
            storage.save(limitKey(userId), overLimit)
        }
    }

    suspend fun setAutoTrackingEnabled(userId: String, enabled: Boolean) {
        if (userId.isEmpty()) return
        storage.save(autoTrackingKey(userId), enabled)
    }

    suspend fun setAutoTrackingPromotionDismissed(userId: String, dismissed: Boolean) {
        if (userId.isEmpty()) return
        storage.save(promotionDismissedKey(userId), dismissed)
    }

    suspend fun clearAllPreferences() {
        storage.clear()
    }
}
