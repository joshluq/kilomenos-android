package es.joshluq.kmsafe.data.repository

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Android implementation of [MediaRepository] using ContentResolver.
 */
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: LoggerKit
) : MediaRepository {

    override suspend fun getFileBytes(uriPath: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val uri = uriPath.toUri()
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            logger.e("MediaRepository", "Failed to read bytes from URI: $uriPath", e)
            null
        }
    }
}
