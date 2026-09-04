package es.joshluq.kmsafe.feature.expenses.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.math.max
import androidx.core.graphics.scale

/**
 * Utility to downscale, orient, and compress receipt images before sending to Gemini 2.5 Flash API.
 * Ensures the output image is <= 1920px on the longest side and JPEG compressed (~82%),
 * keeping the payload size well below 500 KB while preserving sharpness for OCR text extraction.
 */
object ReceiptImageCompressor {

    private const val MAX_DIMENSION = 1920
    private const val JPEG_QUALITY = 82

    suspend fun compress(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
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

            // 6. Save to cache receipts directory
            val receiptsDir = File(context.cacheDir, "receipts").apply { if (!exists()) mkdirs() }
            val outputFile = File(receiptsDir, "receipt_${UUID.randomUUID()}.jpg")

            FileOutputStream(outputFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }

            if (finalBitmap != rawBitmap && !rawBitmap.isRecycled) {
                rawBitmap.recycle()
            }
            if (finalBitmap != orientedBitmap && !orientedBitmap.isRecycled) {
                orientedBitmap.recycle()
            }

            outputFile.absolutePath
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

        val scale = MAX_DIMENSION.toFloat() / maxSide
        val targetWidth = (width * scale).toInt()
        val targetHeight = (height * scale).toInt()
        return bitmap.scale(targetWidth, targetHeight)
    }
}
