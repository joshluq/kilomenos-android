package es.joshluq.kmsafe.infrastructure.local.datasource

import es.joshluq.foundationkit.provider.StorageProvider
import es.joshluq.foundationkit.provider.read
import es.joshluq.foundationkit.provider.save
import es.joshluq.kmsafe.domain.model.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataSource for persistent user settings using Secure StorageProvider.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class PreferencesDataSource @Inject constructor(
    private val storage: StorageProvider
) {
    private val _preferenceUpdates = MutableSharedFlow<Unit>(replay = 1).apply {
        tryEmit(Unit)
    }

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
     * This flow is reactive and will emit new values whenever preferences are updated.
     */
    fun getPreferences(userId: String): Flow<UserPreferences> = _preferenceUpdates.flatMapLatest {
        flow {
            val rememberEmail = storage.read(KEY_REMEMBER_EMAIL) ?: true
            val lastEmail = storage.read(KEY_LAST_EMAIL) ?: ""

            val showBanner = if (userId.isNotEmpty()) {
                storage.read(bannerKey(userId)) ?: true
            } else {
                true
            }

            val lastLimit = if (userId.isNotEmpty()) {
                storage.read<Boolean>(limitKey(userId))
            } else {
                null
            }

            val autoTracking = if (userId.isNotEmpty()) {
                storage.read<Boolean>(autoTrackingKey(userId)) ?: false
            } else {
                false
            }

            val promotionDismissed = if (userId.isNotEmpty()) {
                storage.read<Boolean>(promotionDismissedKey(userId)) ?: false
            } else {
                false
            }

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
    }

    /**
     * Reads global preferences only.
     */
    fun getGlobalPreferences(): Flow<UserPreferences> = _preferenceUpdates.flatMapLatest {
        flow {
            val rememberEmail = storage.read<Boolean>(KEY_REMEMBER_EMAIL) ?: true
            val lastEmail = storage.read<String>(KEY_LAST_EMAIL) ?: ""
            emit(UserPreferences(rememberEmail = rememberEmail, lastEmail = lastEmail))
        }
    }

    suspend fun setRememberEmail(enabled: Boolean) {
        storage.save(KEY_REMEMBER_EMAIL, enabled)
        _preferenceUpdates.tryEmit(Unit)
    }

    suspend fun saveLastEmail(email: String) {
        storage.save(KEY_LAST_EMAIL, email)
        _preferenceUpdates.tryEmit(Unit)
    }

    suspend fun setShowProjectionBanner(userId: String, enabled: Boolean) {
        if (userId.isEmpty()) return
        storage.save(bannerKey(userId), enabled)
        _preferenceUpdates.tryEmit(Unit)
    }

    suspend fun setLastKnownOverLimit(userId: String, overLimit: Boolean?) {
        if (userId.isEmpty()) return
        if (overLimit == null) {
            storage.delete(limitKey(userId))
        } else {
            storage.save(limitKey(userId), overLimit)
        }
        _preferenceUpdates.tryEmit(Unit)
    }

    suspend fun setAutoTrackingEnabled(userId: String, enabled: Boolean) {
        if (userId.isEmpty()) return
        storage.save(autoTrackingKey(userId), enabled)
        _preferenceUpdates.tryEmit(Unit)
    }

    suspend fun setAutoTrackingPromotionDismissed(userId: String, dismissed: Boolean) {
        if (userId.isEmpty()) return
        storage.save(promotionDismissedKey(userId), dismissed)
        _preferenceUpdates.tryEmit(Unit)
    }

    suspend fun clearAllPreferences() {
        storage.clear()
        _preferenceUpdates.tryEmit(Unit)
    }
}
