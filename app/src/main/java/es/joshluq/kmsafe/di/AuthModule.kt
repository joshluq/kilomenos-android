package es.joshluq.kmsafe.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import es.joshluq.authkit.network.sdk.NetworkKit
import es.joshluq.authkit.network.sdk.NetworkKitConfig
import es.joshluq.authkit.sdk.AuthKit
import es.joshluq.authkit.session.model.PersistencePolicy
import es.joshluq.authkit.session.sdk.SessionKit
import es.joshluq.authkit.session.sdk.SessionKitConfig
import es.joshluq.encryptionkit.sdk.EncryptionKit
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.provider.SerializerProvider
import es.joshluq.foundationkit.provider.StorageProvider
import es.joshluq.kmsafe.BuildConfig
import es.joshluq.kmsafe.data.remote.auth.AuthTokenRefresher
import es.joshluq.kmsafe.data.remote.auth.UserSessionDataSource
import es.joshluq.kmsafe.data.remote.auth.UserSessionDataSourceImpl
import es.joshluq.kmsafe.data.util.JacksonSerializerProvider
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Hilt module for providing Authentication related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    private const val STORE_NAME = "kmsafe_auth_store_${BuildConfig.FLAVOR}"
    private const val ENCRYPTION_ALIAS = "kmsafe_secure_key_${BuildConfig.FLAVOR}"
    private const val PREFS_NAME = "kmsafe_secure_prefs_${BuildConfig.FLAVOR}"

    @Provides
    @Singleton
    fun provideAuthKit(
        @ApplicationContext context: Context,
        logger: LoggerKit,
        tokenRefresherProvider: Provider<AuthTokenRefresher>,
        encryptionKit: EncryptionKit
    ): AuthKit {
        return AuthKit.init(context) {
            storeName = STORE_NAME
            this.logger = logger
            this.encryptionKit = encryptionKit
            addFeature(
                SessionKit,
                SessionKitConfig.build {
                    persistence = PersistencePolicy.Persistent
                }
            )
            addFeature(
                NetworkKit,
                NetworkKitConfig.build {
                    tokenRefresher = tokenRefresherProvider.get()
                }
            )
        }
    }

    @Provides
    @Singleton
    fun provideUserSessionDataSource(impl: UserSessionDataSourceImpl): UserSessionDataSource {
        return impl
    }

    @Provides
    @Singleton
    fun provideEncryptionKit(
        @ApplicationContext context: Context,
        logger: LoggerKit
    ): EncryptionKit {
        return EncryptionKit.build(context) {
            alias = ENCRYPTION_ALIAS
            this.logger = logger
        }
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile(PREFS_NAME)
        }
    }

    @Provides
    @Singleton
    fun provideSerializerProvider(impl: JacksonSerializerProvider): SerializerProvider {
        return impl
    }

    @Provides
    @Singleton
    fun provideStorageProvider(
        encryptionKit: EncryptionKit,
        dataStore: DataStore<Preferences>,
        serializerProvider: SerializerProvider
    ): StorageProvider {
        return encryptionKit.createSecureStorage(dataStore, serializerProvider)
    }

}
