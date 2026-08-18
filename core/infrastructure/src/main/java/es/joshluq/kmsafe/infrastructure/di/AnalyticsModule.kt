package es.joshluq.kmsafe.infrastructure.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.kmsafe.infrastructure.analytics.FirebaseAnalyticsProvider
import es.joshluq.kmsafe.infrastructure.analytics.LoggerAnalyticsProvider
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
        val manager = AnalyticskitManager.Builder()
            .addProvider(loggerProvider)
            .addProvider(firebaseProvider)
            .build()

        // Configure Global Properties for all events
        manager.apply {
            addGlobalProperty("device_model", android.os.Build.MODEL)
            addGlobalProperty("device_brand", android.os.Build.MANUFACTURER)
            addGlobalProperty("os_version", android.os.Build.VERSION.RELEASE)
            addGlobalProperty("android_api", android.os.Build.VERSION.SDK_INT)
        }

        return manager
    }
}
