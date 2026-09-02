package es.joshluq.kmsafe.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.kmsafe.auth.AuthConfigImpl
import es.joshluq.kmsafe.core.monetization.domain.MonetizationConfig
import es.joshluq.kmsafe.feature.auth.domain.AuthConfig
import es.joshluq.kmsafe.feature.profile.domain.ProfileConfig
import es.joshluq.kmsafe.monetization.MonetizationConfigImpl
import es.joshluq.kmsafe.profile.ProfileConfigImpl
import javax.inject.Singleton

/**
 * Hilt module responsible for providing environment-specific configurations
 * to feature and core modules via their respective interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FeatureConfigModule {

    @Binds
    @Singleton
    abstract fun bindMonetizationConfig(
        impl: MonetizationConfigImpl
    ): MonetizationConfig

    @Binds
    @Singleton
    abstract fun bindAuthConfig(
        impl: AuthConfigImpl
    ): AuthConfig

    @Binds
    @Singleton
    abstract fun bindProfileConfig(
        impl: ProfileConfigImpl
    ): ProfileConfig
}
