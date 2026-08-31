package es.joshluq.kmsafe.auth

import es.joshluq.kmsafe.BuildConfig
import es.joshluq.kmsafe.feature.auth.domain.AuthConfig
import javax.inject.Inject

class AuthConfigImpl @Inject constructor() : AuthConfig {
    override fun getTermsUrl(): String = BuildConfig.TERMS_URL
    override fun getPrivacyUrl(): String = BuildConfig.PRIVACY_URL
}
