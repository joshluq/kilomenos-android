package es.joshluq.kmsafe.infrastructure

import es.joshluq.kmsafe.BuildConfig
import javax.inject.Inject

class InfrastructureConfigImpl @Inject constructor() : InfrastructureConfig {
    override val serverUrl: String = BuildConfig.SERVER_URL
    override val apiKey: String = BuildConfig.API_KEY
    override val storageUrl: String = BuildConfig.STORAGE_URL
    override val databaseName: String = "kmsafe_db_${BuildConfig.FLAVOR}"
    override val authStoreName: String = "kmsafe_auth_store_${BuildConfig.FLAVOR}"
    override val encryptionAlias: String = "kmsafe_secure_key_${BuildConfig.FLAVOR}"
    override val securePrefsName: String = "kmsafe_secure_prefs_${BuildConfig.FLAVOR}"
    override val googleWebClientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID
    override val premiumSku: String = BuildConfig.PREMIUM_SKU
    override val isDebug: Boolean = BuildConfig.DEBUG
}
