package es.joshluq.kmsafe.core.infrastructure.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.kmsafe.core.domain.service.GeofenceService
import es.joshluq.kmsafe.core.infrastructure.service.GeofenceServiceImpl
import es.joshluq.kmsafe.domain.service.FingerprintProvider
import es.joshluq.kmsafe.domain.service.SocialAuthService
import es.joshluq.kmsafe.infrastructure.remote.auth.GoogleAuthManager
import es.joshluq.kmsafe.infrastructure.util.DeviceFingerprintProvider
import javax.inject.Singleton

import es.joshluq.kmsafe.domain.service.BillingService
import es.joshluq.kmsafe.infrastructure.remote.billing.BillingManager

/**
 * Hilt module for binding infrastructure services.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    @Singleton
    abstract fun bindGeofenceService(
        service: GeofenceServiceImpl
    ): GeofenceService

    @Binds
    @Singleton
    abstract fun bindFingerprintProvider(
        provider: DeviceFingerprintProvider
    ): FingerprintProvider

    @Binds
    @Singleton
    abstract fun bindSocialAuthService(
        service: GoogleAuthManager
    ): SocialAuthService

    @Binds
    @Singleton
    abstract fun bindBillingService(
        manager: BillingManager
    ): BillingService
}
