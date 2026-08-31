package es.joshluq.kmsafe.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.kmsafe.infrastructure.InfrastructureConfig
import es.joshluq.kmsafe.infrastructure.InfrastructureConfigImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ConfigModule {

    @Binds
    @Singleton
    abstract fun bindInfrastructureConfig(
        impl: InfrastructureConfigImpl
    ): InfrastructureConfig
}
