package es.joshluq.kmsafe.infrastructure

/**
 * Interface providing infrastructure-level configuration (API URLs, database names, etc.).
 * Allows the core:infrastructure module to remain agnostic of build flavors and secrets.
 */
interface InfrastructureConfig {
    val serverUrl: String
    val apiKey: String
    val storageUrl: String
    val databaseName: String
    val authStoreName: String
    val encryptionAlias: String
    val securePrefsName: String
    val googleWebClientId: String
    val premiumSku: String
    val isDebug: Boolean
}
