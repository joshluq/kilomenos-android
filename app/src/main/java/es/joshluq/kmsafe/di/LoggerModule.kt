package es.joshluq.kmsafe.di

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.joshluq.foundationkit.log.LogLevel
import es.joshluq.foundationkit.log.LogProvider
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.BuildConfig
import es.joshluq.kmsafe.data.log.CrashlyticsLogProvider
import javax.inject.Singleton

/**
 * Hilt module for providing Logging dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object LoggerModule {

    @Provides
    @Singleton
    fun provideLogger(): LoggerKit {
        val level = if (BuildConfig.DEBUG) {
            LogLevel.VERBOSE
        } else {
            LogLevel.NONE
        }

        return LoggerKit.Builder()
            .setProvider(
                CompositeLogProvider(
                    listOf(
                        KmLogProvider(level),
                        CrashlyticsLogProvider()
                    )
                )
            )
            .build()
    }

    private class CompositeLogProvider(
        private val providers: List<LogProvider>
    ) : LogProvider {
        override val minLogLevel: LogLevel = LogLevel.VERBOSE // Managed by children

        override fun log(priority: LogLevel, tag: String, message: String, throwable: Throwable?) {
            providers.forEach { it.log(priority, tag, message, throwable) }
        }
    }

    private class KmLogProvider(
        override val minLogLevel: LogLevel,
    ) : LogProvider {
        override fun log(priority: LogLevel, tag: String, message: String, throwable: Throwable?) {
            if (priority.priority < minLogLevel.priority) return

            val appTag = "KiloMenos"
            val decoratedMessage = decorateMessage(tag, priority, message)

            when (priority) {
                LogLevel.VERBOSE -> Log.v(appTag, decoratedMessage, throwable)
                LogLevel.DEBUG -> Log.d(appTag, decoratedMessage, throwable)
                LogLevel.INFO -> Log.i(appTag, decoratedMessage, throwable)
                LogLevel.WARN -> Log.w(appTag, decoratedMessage, throwable)
                LogLevel.ERROR -> Log.e(appTag, decoratedMessage, throwable)
                LogLevel.ASSERT -> Log.wtf(appTag, decoratedMessage, throwable)
                LogLevel.NONE -> { /* No-op */ }
            }
        }

        private fun decorateMessage(tag: String, priority: LogLevel, message: String): String {
            val emoji = "${priority.emoji} "
            return "$emoji[$tag] $message"
        }
    }
}
