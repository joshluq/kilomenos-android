package es.joshluq.kmsafe.feature.auth.domain

/**
 * Interface providing authentication-related configuration (legal URLs, etc.).
 * Allows the Auth feature module to remain agnostic of build flavors.
 */
interface AuthConfig {
    /** Returns the URL for the Terms & Conditions. */
    fun getTermsUrl(): String

    /** Returns the URL for the Privacy Policy. */
    fun getPrivacyUrl(): String
}
