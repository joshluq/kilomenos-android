package es.joshluq.kmsafe.core.monetization.domain

/**
 * Contract for providing monetization-related configuration values.
 * This abstraction allows feature modules to be environment-agnostic.
 */
interface MonetizationConfig {
    /**
     * Returns the AdMob Banner ID for the Overview screen.
     */
    fun getOverviewBannerAdUnitId(): String

    /**
     * Returns the AdMob Banner ID for the History screen.
     */
    fun getHistoryBannerAdUnitId(): String

    /**
     * Returns the AdMob Banner ID for the Expenses screen.
     */
    fun getExpensesBannerAdUnitId(): String

    /**
     * Returns the AdMob Banner ID for the Projection screen.
     */
    fun getProjectionBannerAdUnitId(): String

    /**
     * Legacy fallback for generic banner placements.
     */
    fun getBannerAdUnitId(): String = getOverviewBannerAdUnitId()
}

