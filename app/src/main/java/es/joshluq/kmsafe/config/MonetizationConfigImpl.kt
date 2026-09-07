package es.joshluq.kmsafe.config

import es.joshluq.kmsafe.BuildConfig
import es.joshluq.kmsafe.core.monetization.domain.MonetizationConfig
import javax.inject.Inject

/**
 * Implementation of [es.joshluq.kmsafe.core.monetization.domain.MonetizationConfig] using the Shell module's BuildConfig.
 * This effectively injects the environment-specific secrets into the core/feature modules.
 */
class MonetizationConfigImpl @Inject constructor() : MonetizationConfig {
    override fun getOverviewBannerAdUnitId(): String {
        return BuildConfig.ADMOB_OVERVIEW_BANNER_ID
    }

    override fun getHistoryBannerAdUnitId(): String {
        return BuildConfig.ADMOB_HISTORY_BANNER_ID
    }

    override fun getExpensesBannerAdUnitId(): String {
        return BuildConfig.ADMOB_EXPENSES_BANNER_ID
    }

    override fun getProjectionBannerAdUnitId(): String {
        return BuildConfig.ADMOB_PROJECTION_BANNER_ID
    }

    override fun getBannerAdUnitId(): String {
        return getOverviewBannerAdUnitId()
    }
}