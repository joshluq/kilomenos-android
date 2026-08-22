package es.joshluq.kmsafe.core.infrastructure.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.kmsafe.core.domain.service.GeofenceService
import es.joshluq.kmsafe.core.infrastructure.service.GeofenceServiceImpl
import javax.inject.Singleton

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
}
