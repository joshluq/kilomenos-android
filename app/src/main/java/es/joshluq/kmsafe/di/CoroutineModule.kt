package es.joshluq.kmsafe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.foundationkit.coroutines.DefaultDispatcherProvider
import es.joshluq.foundationkit.coroutines.DispatcherProvider
import javax.inject.Singleton

/**
 * Hilt module to provide Coroutine related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider {
        return DefaultDispatcherProvider()
    }
}
