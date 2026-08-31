package es.joshluq.kmsafe.core.monetization.domain

/**
 * Contract for providing monetization-related configuration values.
 * This abstraction allows feature modules to be environment-agnostic.
 */
interface MonetizationConfig {
    /**
     * Returns the AdMob Banner ID for the current build environment.
     */
    fun getBannerAdUnitId(): String
}
