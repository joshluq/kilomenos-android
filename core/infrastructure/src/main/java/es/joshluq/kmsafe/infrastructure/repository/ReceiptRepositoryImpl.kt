package es.joshluq.kmsafe.infrastructure.repository

import es.joshluq.foundationkit.coroutines.DispatcherProvider
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.domain.model.ReceiptScanResult
import es.joshluq.kmsafe.domain.repository.ReceiptRepository
import es.joshluq.kmsafe.infrastructure.mapper.ErrorMapper
import es.joshluq.kmsafe.infrastructure.remote.api.ReceiptsApiService
import es.joshluq.kmsafe.infrastructure.remote.api.StorageApiService
import es.joshluq.kmsafe.infrastructure.remote.dto.ProcessReceiptRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Infrastructure implementation of [ReceiptRepository] using Supabase Storage and Edge Functions.
 */
@Singleton
class ReceiptRepositoryImpl @Inject constructor(
    private val storageApiService: StorageApiService,
    private val receiptsApiService: ReceiptsApiService,
    private val errorMapper: ErrorMapper,
    private val logger: LoggerKit,
    private val dispatchers: DispatcherProvider
) : ReceiptRepository {

    override fun uploadReceiptImage(
        userId: String,
        fileName: String,
        imageBytes: ByteArray,
        mimeType: String
    ): Flow<String> = flow {
        logger.d("ReceiptRepository", "Uploading receipt image for user: $userId, file: $fileName")
        val requestBody = imageBytes.toRequestBody(mimeType.toMediaTypeOrNull())

        val response = storageApiService.uploadReceipt(userId, fileName, requestBody, upsert = "true")
        val code = response.code()
        val isSuccessful = response.isSuccessful
        val errorBody = if (!isSuccessful) response.errorBody()?.string().orEmpty() else ""

        if (isSuccessful || errorBody.contains("KeyAlreadyExists", ignoreCase = true) || code == 409) {
            val relativePath = "$userId/$fileName"
            logger.i("ReceiptRepository", "Receipt uploaded successfully: $relativePath")
            emit(relativePath)
        } else {
            logger.e("ReceiptRepository", "Receipt upload failed with code: $code, body: $errorBody")
            throw KmException(KmError.NetworkError)
        }
    }.flowOn(dispatchers.io)

    override fun deleteReceiptImage(filePath: String): Flow<Unit> = flow {
        logger.d("ReceiptRepository", "Deleting receipt image: $filePath")
        val segments = filePath.split("/")
        if (segments.size < 2) {
            logger.w("ReceiptRepository", "Invalid receipt file path structure: $filePath")
            emit(Unit)
            return@flow
        }

        val userId = segments[0]
        val fileName = segments.subList(1, segments.size).joinToString("/")

        val response = storageApiService.deleteReceipt(userId, fileName)
        if (response.isSuccessful || response.code() == 404) {
            logger.i("ReceiptRepository", "Receipt image deleted successfully (or already non-existent): $filePath")
            emit(Unit)
        } else {
            val errorBody = response.errorBody()?.string().orEmpty()
            logger.w("ReceiptRepository", "Delete receipt failed with code: ${response.code()}, body: $errorBody")
            emit(Unit) // Non-fatal on discard fallback
        }
    }.flowOn(dispatchers.io)

    override fun processReceipt(filePath: String): Flow<ReceiptScanResult> = flow {
        logger.d("ReceiptRepository", "Processing receipt via OCR Edge Function: $filePath")
        val response = receiptsApiService.processReceipt(ProcessReceiptRequest(filePath))

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.success && body.data != null) {
                val scanResult = body.data.toDomain(filePath)
                logger.i("ReceiptRepository", "OCR processing successful: station=${scanResult.stationName}, total=${scanResult.totalAmount}")
                emit(scanResult)
            } else {
                val errorMsg = body?.error ?: "Receipt could not be processed"
                logger.w("ReceiptRepository", "OCR processing rejected by server: $errorMsg")
                throw KmException(KmError.InvalidFuelExpenseValues)
            }
        } else {
            logger.e("ReceiptRepository", "OCR process failed with code: ${response.code()}")
            throw KmException(errorMapper.mapApiResponse(response))
        }
    }.flowOn(dispatchers.io)
}
