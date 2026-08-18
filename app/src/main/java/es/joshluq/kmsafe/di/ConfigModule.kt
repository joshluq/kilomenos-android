package es.joshluq.kmsafe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.kmsafe.BuildConfig
import es.joshluq.kmsafe.infrastructure.InfrastructureConfig
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Provides
    @Singleton
    fun provideInfrastructureConfig(): InfrastructureConfig {
        return InfrastructureConfig(
            serverUrl = BuildConfig.SERVER_URL,
            apiKey = BuildConfig.API_KEY,
            storageUrl = BuildConfig.STORAGE_URL,
            databaseName = "kmsafe_db_${BuildConfig.FLAVOR}",
            authStoreName = "kmsafe_auth_store_${BuildConfig.FLAVOR}",
            encryptionAlias = "kmsafe_secure_key_${BuildConfig.FLAVOR}",
            securePrefsName = "kmsafe_secure_prefs_${BuildConfig.FLAVOR}",
            googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
            premiumSku = BuildConfig.PREMIUM_SKU,
            isDebug = BuildConfig.DEBUG
        )
    }
}
