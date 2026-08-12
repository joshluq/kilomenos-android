package es.joshluq.kmsafe.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.MainActivity
import es.joshluq.kmsafe.R
import es.joshluq.kmsafe.domain.repository.HistoryRepository
import es.joshluq.kmsafe.domain.repository.RentingRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Worker that checks for user inactivity and sends a reminder notification.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val rentingRepository: RentingRepository,
    private val historyRepository: HistoryRepository,
    private val logger: LoggerKit
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "reminder_channel_${es.joshluq.kmsafe.BuildConfig.FLAVOR}"
        private const val NOTIFICATION_ID = 2001
        private const val INACTIVITY_THRESHOLD_DAYS = 3L
    }

    override suspend fun doWork(): Result {
        logger.d("ReminderWorker", "Starting inactivity check...")

        return try {
            val contract = rentingRepository.getContract().first()
            if (contract == null) {
                logger.d("ReminderWorker", "No contract found. Skipping.")
                return Result.success()
            }

            val history = historyRepository.getHistory(contract.id).first()
            val lastRecord = history.firstOrNull()

            val lastTimestamp = lastRecord?.timestamp ?: contract.startDate
            val currentTime = System.currentTimeMillis()
            val diffMillis = currentTime - lastTimestamp
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

            if (diffDays >= INACTIVITY_THRESHOLD_DAYS) {
                logger.i("ReminderWorker", "User inactive for $diffDays days. Sending notification.")
                sendNotification()
            } else {
                logger.d("ReminderWorker", "User active. Days since last update: $diffDays")
            }

            Result.success()
        } catch (e: Exception) {
            logger.e("ReminderWorker", "Error during inactivity check", e)
            Result.retry()
        }
    }

    private fun sendNotification() {
        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = applicationContext.getString(R.string.reminder_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(R.string.reminder_notification_title))
            .setContentText(applicationContext.getString(R.string.reminder_notification_message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
