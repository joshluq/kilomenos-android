package es.joshluq.kmsafe.infrastructure

data class InfrastructureConfig(
    val serverUrl: String,
    val apiKey: String,
    val storageUrl: String,
    val databaseName: String,
    val authStoreName: String,
    val encryptionAlias: String,
    val securePrefsName: String,
    val googleWebClientId: String,
    val premiumSku: String,
    val isDebug: Boolean
)
