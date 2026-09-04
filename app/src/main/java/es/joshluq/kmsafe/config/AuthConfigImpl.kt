package es.joshluq.kmsafe.config

import es.joshluq.kmsafe.BuildConfig
import es.joshluq.kmsafe.feature.auth.domain.AuthConfig

class AuthConfigImpl @Inject constructor() : AuthConfig {
    override fun getTermsUrl(): String = BuildConfig.TERMS_URL
    override fun getPrivacyUrl(): String = BuildConfig.PRIVACY_URL
}