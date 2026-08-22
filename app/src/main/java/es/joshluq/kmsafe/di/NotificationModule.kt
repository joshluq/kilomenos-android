package es.joshluq.kmsafe.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.kmsafe.core.domain.service.StationNotificationService
import es.joshluq.kmsafe.data.notification.StationNotificationServiceImpl
import javax.inject.Singleton

/**
 * Hilt module for binding notification services.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindStationNotificationService(
        service: StationNotificationServiceImpl
    ): StationNotificationService
}
