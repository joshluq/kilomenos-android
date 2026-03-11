package es.joshluq.kmsafe.domain.repository

import es.joshluq.kmsafe.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user preferences.
 */
interface PreferencesRepository {
    /**
     * Observes the current user preferences for a specific user.
     */
    fun getPreferences(userId: String): Flow<UserPreferences>

    /**
     * Observes the global device preferences (email, etc).
     */
    fun getGlobalPreferences(): Flow<UserPreferences>

    /**
     * Updates the 'rememberEmail' flag.
     */
    suspend fun setRememberEmail(enabled: Boolean)

    /**
     * Updates the last used email.
     */
    suspend fun saveLastEmail(email: String)

    /**
     * Updates the projection banner visibility for a specific user.
     */
    suspend fun setShowProjectionBanner(userId: String, enabled: Boolean)

    /**
     * Updates the last known state of the projection for a specific user.
     */
    suspend fun setLastKnownOverLimit(userId: String, overLimit: Boolean?)

    /**
     * Clears all stored preferences (global and user-specific).
     */
    suspend fun clearPreferences()
}
