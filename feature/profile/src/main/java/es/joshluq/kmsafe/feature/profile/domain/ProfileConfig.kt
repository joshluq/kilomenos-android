package es.joshluq.kmsafe.feature.profile.domain

/**
 * Interface providing configuration for the Profile feature.
 */
interface ProfileConfig {
    /** Returns the URL for the Terms & Conditions. */
    fun getTermsUrl(): String

    /** Returns the URL for the Privacy Policy. */
    fun getPrivacyUrl(): String
}
