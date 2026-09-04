package es.joshluq.kmsafe.config

import es.joshluq.kmsafe.BuildConfig
import es.joshluq.kmsafe.feature.profile.domain.ProfileConfig
import javax.inject.Inject

class ProfileConfigImpl @Inject constructor() : ProfileConfig {
    override fun getTermsUrl(): String = BuildConfig.TERMS_URL
    override fun getPrivacyUrl(): String = BuildConfig.PRIVACY_URL
}