package es.joshluq.kmsafe.core.analytics.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.kmsafe.core.analytics.AnalyticsTracker
import es.joshluq.kmsafe.core.analytics.impl.AnalyticsTrackerImpl
import es.joshluq.kmsafe.core.analytics.provider.FirebaseAnalyticsProvider
import es.joshluq.kmsafe.core.analytics.provider.LoggerAnalyticsProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsBindingModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsTracker(impl: AnalyticsTrackerImpl): AnalyticsTracker
}

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsCoreModule {

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

        manager.apply {
            addGlobalProperty("device_model", android.os.Build.MODEL)
            addGlobalProperty("device_brand", android.os.Build.MANUFACTURER)
            addGlobalProperty("os_version", android.os.Build.VERSION.RELEASE)
            addGlobalProperty("android_api", android.os.Build.VERSION.SDK_INT)
        }

        return manager
    }
}
