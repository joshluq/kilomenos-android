package es.joshluq.kmsafe.data.log

import com.google.firebase.crashlytics.FirebaseCrashlytics
import es.joshluq.foundationkit.log.LogLevel
import es.joshluq.foundationkit.log.LogProvider

/**
 * A [LogProvider] that forwards logs to Firebase Crashlytics as custom logs and non-fatal exceptions.
 *
 * This provider is designed for remote debugging of background processes.
 */
class CrashlyticsLogProvider(
    override val minLogLevel: LogLevel = LogLevel.ERROR
) : LogProvider {

    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun log(priority: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (priority.priority < minLogLevel.priority) return

        val decoratedMessage = "(${priority.emoji}) [$tag] $message"

        crashlytics.log(decoratedMessage)

        if (throwable != null) {
            crashlytics.setCustomKey("log_tag", tag)
            crashlytics.setCustomKey("log_priority", priority.name)
            crashlytics.recordException(throwable)
        }
    }
}
