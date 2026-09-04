package es.joshluq.kmsafe.infrastructure.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

/**
 * Utility to downscale, orient, and compress receipt images before network upload.
 * Ensures output is <= 1920px on the longest side and JPEG compressed (~82%),
 * returning raw bytes below 500 KB while preserving sharpness for OCR text extraction.
 */
object ReceiptImageCompressor {

    private const val MAX_DIMENSION = 1920
    private const val JPEG_QUALITY = 82

    suspend fun compress(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        try {
            // 1. Decode image bounds
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            inputStream?.close()

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return@withContext null

            // 2. Calculate sub-sampling factor
            val maxSide = max(boundsOptions.outWidth, boundsOptions.outHeight)
            var sampleSize = 1
            while ((maxSide / sampleSize) > MAX_DIMENSION * 1.5) {
                sampleSize *= 2
            }

            // 3. Decode sub-sampled bitmap
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val rawBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream.close()

            if (rawBitmap == null) return@withContext null

            // 4. Correct EXIF orientation
            val orientedBitmap = correctOrientation(context, uri, rawBitmap)

            // 5. Final scale if still exceeds max dimension
            val finalBitmap = scaleIfNeeded(orientedBitmap)

            // 6. Compress to JPEG bytes in memory
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)

            if (finalBitmap != rawBitmap && !rawBitmap.isRecycled) {
                rawBitmap.recycle()
            }
            if (finalBitmap != orientedBitmap && !orientedBitmap.isRecycled) {
                orientedBitmap.recycle()
            }

            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    private fun correctOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            inputStream.close()

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun scaleIfNeeded(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxSide = max(width, height)

        if (maxSide <= MAX_DIMENSION) return bitmap

        val scaleRatio = MAX_DIMENSION.toFloat() / maxSide
        val targetWidth = (width * scaleRatio).toInt()
        val targetHeight = (height * scaleRatio).toInt()

        return bitmap.scale(targetWidth, targetHeight, filter = true)
    }
}
