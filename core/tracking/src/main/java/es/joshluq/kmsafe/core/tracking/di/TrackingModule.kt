package es.joshluq.kmsafe.core.tracking.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.kmsafe.core.tracking.TrackingServiceControllerImpl
import es.joshluq.kmsafe.domain.service.TrackingServiceController
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TrackingModule {

    @Binds
    @Singleton
    abstract fun bindTrackingServiceController(
        impl: TrackingServiceControllerImpl
    ): TrackingServiceController
}
