package es.joshluq.kmsafe.data.repository

import es.joshluq.kmsafe.data.local.datasource.PreferencesDataSource
import es.joshluq.kmsafe.domain.model.UserPreferences
import es.joshluq.kmsafe.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [PreferencesRepository] using [PreferencesDataSource].
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataSource: PreferencesDataSource
) : PreferencesRepository {

    override fun getPreferences(userId: String): Flow<UserPreferences> {
        return dataSource.getPreferences(userId)
    }

    override fun getGlobalPreferences(): Flow<UserPreferences> {
        return dataSource.getGlobalPreferences()
    }

    override suspend fun setRememberEmail(enabled: Boolean) {
        dataSource.setRememberEmail(enabled)
    }

    override suspend fun saveLastEmail(email: String) {
        dataSource.saveLastEmail(email)
    }

    override suspend fun setShowProjectionBanner(userId: String, enabled: Boolean) {
        dataSource.setShowProjectionBanner(userId, enabled)
    }

    override suspend fun setLastKnownOverLimit(userId: String, overLimit: Boolean?) {
        dataSource.setLastKnownOverLimit(userId, overLimit)
    }

    override suspend fun clearPreferences() {
        dataSource.clearAllPreferences()
    }
}
