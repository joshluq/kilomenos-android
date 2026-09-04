package es.joshluq.kmsafe.infrastructure.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.usecase.MigrateLocalDataToRemoteUseCase
import kotlinx.coroutines.flow.last

/**
 * Background worker that synchronizes pending local data with the remote server.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val migrateLocalDataToRemoteUseCase: MigrateLocalDataToRemoteUseCase,
    private val logger: LoggerKit
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        logger.d("SyncWorker", "Starting background data synchronization")

        return try {
            val output = migrateLocalDataToRemoteUseCase(MigrateLocalDataToRemoteUseCase.Input).last()

            if (output is MigrateLocalDataToRemoteUseCase.Output.Success) {
                logger.i("SyncWorker", "Synchronization completed successfully")
                Result.success()
            } else {
                logger.w("SyncWorker", "Synchronization failed, retrying...")
                Result.retry()
            }
        } catch (e: Exception) {
            logger.e("SyncWorker", "Critical error during synchronization", e)
            Result.failure()
        }
    }
}
