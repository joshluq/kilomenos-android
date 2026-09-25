package es.joshluq.kmsafe.infrastructure.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import es.joshluq.kmsafe.domain.model.PremiumRequiredException
import es.joshluq.kmsafe.domain.repository.NotificationRepository
import java.util.concurrent.TimeUnit

/**
 * Background worker responsible for bidirectional synchronization of notifications:
 * 1. Pushes locally generated pending notifications to Supabase v1 /v1/notifications/sync
 * 2. Fetches remote notifications from /v1/notifications
 *
 * Runs with [NetworkType.CONNECTED] constraint.
 */
@HiltWorker
class SyncNotificationsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationRepository: NotificationRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Sync pending local notifications to remote
            val syncSuccess = notificationRepository.syncPending()

            // 2. Fetch remote notifications
            val fetchResult = notificationRepository.fetchRemoteNotifications()

            if (syncSuccess) {
                Result.success()
            } else {
                // If fetch failed due to Premium restriction, do not retry indefinitely
                if (fetchResult.exceptionOrNull() is PremiumRequiredException) {
                    Result.success()
                } else {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "SyncNotificationsWork"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncNotificationsWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
