package es.joshluq.kmsafe.domain.repository

import es.joshluq.kmsafe.domain.model.ReceiptScanResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for receipt image storage and AI OCR processing.
 *
 * Follows Clean Architecture and domain purity with zero Android SDK imports.
 */
interface ReceiptRepository {

    /**
     * Uploads receipt image bytes to the private storage bucket.
     *
     * @param userId The ID of the authenticated user (for RLS path prefix).
     * @param fileName Unique file name (e.g. receipt-1725482938102.jpg).
     * @param imageBytes Raw bytes of the compressed image.
     * @param mimeType MIME type of the image.
     * @return Flow emitting the relative storage file path (e.g. "userId/receipt-123.jpg").
     */
    fun uploadReceiptImage(
        userId: String,
        fileName: String,
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): Flow<String>

    /**
     * Deletes a receipt image from storage.
     *
     * @param filePath The relative storage file path (e.g. "userId/receipt-123.jpg").
     * @return Flow completing when deleted.
     */
    fun deleteReceiptImage(filePath: String): Flow<Unit>

    /**
     * Invokes the OCR extraction service for a receipt previously uploaded to storage.
     *
     * @param filePath The relative storage file path in the receipts bucket.
     * @return Flow emitting the extracted [ReceiptScanResult].
     */
    fun processReceipt(filePath: String): Flow<ReceiptScanResult>
}
