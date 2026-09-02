package es.joshluq.kmsafe

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.foundationkit.usecase.FlowUseCase
import es.joshluq.kmsafe.data.worker.ReminderWorker
import es.joshluq.kmsafe.domain.di.GetPreferences
import es.joshluq.kmsafe.domain.usecase.GetPreferencesUseCase
import es.joshluq.kmsafe.infrastructure.repository.tracking.AutoTrackingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class KiloMenosApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var logger: LoggerKit

    @Inject
    lateinit var autoTrackingManager: AutoTrackingManager

    @Inject
    @GetPreferences
    lateinit var getPreferencesUseCase:
        @JvmSuppressWildcards FlowUseCase<GetPreferencesUseCase.Input, GetPreferencesUseCase.Output>

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        logger.i("Application", "KiloMenos started. Version: ${BuildConfig.VERSION_NAME}")
        setupBackgroundWorkers()
        initializeAutoTracking()
    }

    private fun setupBackgroundWorkers() {
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SmartReminderWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun initializeAutoTracking() {
        applicationScope.launch {
            try {
                val output = getPreferencesUseCase(GetPreferencesUseCase.Input).first()
                if (output is GetPreferencesUseCase.Output.Success && output.preferences.autoTrackingEnabled) {
                    logger.i("Application", "Auto-tracking is enabled in preferences. Ensuring registration.")
                    autoTrackingManager.startAutoTracking()
                }
            } catch (e: Exception) {
                logger.e("Application", "Failed to initialize auto-tracking on start", e)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
