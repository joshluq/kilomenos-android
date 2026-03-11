package es.joshluq.kmsafe.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.kmsafe.data.analytics.FirebaseAnalyticsProvider
import es.joshluq.kmsafe.data.analytics.LoggerAnalyticsProvider
import javax.inject.Singleton

/**
 * Hilt module for providing Analytics dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsManager(
        loggerProvider: LoggerAnalyticsProvider,
        firebaseProvider: FirebaseAnalyticsProvider
    ): AnalyticskitManager {
        return AnalyticskitManager.Builder()
            .addProvider(loggerProvider)
            .addProvider(firebaseProvider)
            .build()
    }
}
