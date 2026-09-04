package es.joshluq.kmsafe.config

import es.joshluq.kmsafe.BuildConfig
import es.joshluq.kmsafe.core.monetization.domain.MonetizationConfig
import javax.inject.Inject

/**
 * Implementation of [es.joshluq.kmsafe.core.monetization.domain.MonetizationConfig] using the Shell module's BuildConfig.
 * This effectively injects the environment-specific secrets into the core/feature modules.
 */
class MonetizationConfigImpl @Inject constructor() : MonetizationConfig {
    override fun getBannerAdUnitId(): String {
        return BuildConfig.ADMOB_BANNER_ID
    }
}