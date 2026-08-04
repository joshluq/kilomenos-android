package es.joshluq.kmsafe.data.log

import com.google.firebase.crashlytics.FirebaseCrashlytics
import es.joshluq.foundationkit.log.LogLevel
import es.joshluq.foundationkit.log.LogProvider
import es.joshluq.kmsafe.BuildConfig

/**
 * A [LogProvider] that forwards logs to Firebase Crashlytics as custom logs and non-fatal exceptions.
 *
 * This provider is designed for remote debugging of background processes.
 */
class CrashlyticsLogProvider(
    override val minLogLevel: LogLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.ERROR
) : LogProvider {

    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun log(priority: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (priority.priority < minLogLevel.priority) return

        val decoratedMessage = "(${priority.emoji}) [$tag] $message"

        // Add to Crashlytics Log (Breadcrumbs)
        crashlytics.log(decoratedMessage)

        // For Debugging purposes during this phase, we treat DEBUG+ logs as non-fatals 
        // to force an immediate upload when a session ends or a crash occurs.
        if (priority.priority >= LogLevel.INFO.priority || BuildConfig.DEBUG) {
            val exception = throwable ?: Exception("Remote Log: $message")
            crashlytics.setCustomKey("log_tag", tag)
            crashlytics.setCustomKey("log_priority", priority.name)
            crashlytics.recordException(exception)
        }
    }
}
