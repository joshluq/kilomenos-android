package es.joshluq.kmsafe.infrastructure.mapper

import es.joshluq.kmsafe.domain.model.KmError
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ErrorMapperTest {

    private val mapper = ErrorMapper()

    @Test
    fun `given RATE_LIMIT_BURST_EXCEEDED response when mapApiResponse then returns ReceiptScanRateLimitBurst with parsed seconds`() {
        val json = """
            {
              "success": false,
              "error": "RATE_LIMIT_BURST_EXCEEDED",
              "message": "Has alcanzado el límite de 3 escaneos cada 5 minutos.",
              "retry_after_seconds": 45
            }
        """.trimIndent()
        val response = Response.error<Any>(429, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertTrue(error is KmError.ReceiptScanRateLimitBurst)
        assertEquals(45, (error as KmError.ReceiptScanRateLimitBurst).retryAfterSeconds)
    }

    @Test
    fun `given RATE_LIMIT_DAILY_EXCEEDED response when mapApiResponse then returns ReceiptScanRateLimitDaily`() {
        val json = """
            {
              "success": false,
              "error": "RATE_LIMIT_DAILY_EXCEEDED",
              "message": "Has alcanzado el límite diario de 20 escaneos."
            }
        """.trimIndent()
        val response = Response.error<Any>(429, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.ReceiptScanRateLimitDaily, error)
    }

    @Test
    fun `given RATE_LIMIT_INVALID_DOCS_COOLDOWN response when mapApiResponse then returns ReceiptScanRateLimitInvalidDocs`() {
        val json = """
            {
              "success": false,
              "error": "RATE_LIMIT_INVALID_DOCS_COOLDOWN",
              "message": "Se detectaron múltiples documentos no válidos.",
              "retry_after_seconds": 300
            }
        """.trimIndent()
        val response = Response.error<Any>(429, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertTrue(error is KmError.ReceiptScanRateLimitInvalidDocs)
        assertEquals(300, (error as KmError.ReceiptScanRateLimitInvalidDocs).retryAfterSeconds)
    }

    @Test
    fun `given PREMIUM_FEATURE_REQUIRED response when mapApiResponse then returns FuelExpensesPremiumOnly`() {
        val json = """
            {
              "success": false,
              "error": "PREMIUM_FEATURE_REQUIRED",
              "message": "El escaneo de tickets mediante IA es una funcionalidad exclusiva de KmSafe Premium."
            }
        """.trimIndent()
        val response = Response.error<Any>(403, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.FuelExpensesPremiumOnly, error)
    }

    @Test
    fun `given DOCUMENT_NOT_A_FUEL_RECEIPT response when mapApiResponse then returns InvalidReceiptImage`() {
        val json = """
            {
              "success": false,
              "error": "DOCUMENT_NOT_A_FUEL_RECEIPT",
              "message": "El documento no corresponde a un ticket de repostaje."
            }
        """.trimIndent()
        val response = Response.error<Any>(400, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.InvalidReceiptImage, error)
    }

    @Test
    fun `given UNSUPPORTED_MEDIA_TYPE response when mapApiResponse then returns UnsupportedImageFormat`() {
        val json = """
            {
              "success": false,
              "error": "UNSUPPORTED_MEDIA_TYPE",
              "message": "Formato de imagen no compatible."
            }
        """.trimIndent()
        val response = Response.error<Any>(400, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.UnsupportedImageFormat, error)
    }

    @Test
    fun `given UNAUTHORIZED_FILE_ACCESS response when mapApiResponse then returns UnauthorizedFileAccess`() {
        val json = """
            {
              "success": false,
              "error": "UNAUTHORIZED_FILE_ACCESS",
              "message": "No tienes autorización."
            }
        """.trimIndent()
        val response = Response.error<Any>(403, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.UnauthorizedFileAccess, error)
    }

    @Test
    fun `given 401 UNAUTHORIZED response when mapApiResponse then returns Unauthenticated`() {
        val json = """
            {
              "success": false,
              "error": "INVALID_TOKEN",
              "message": "Token expirado."
            }
        """.trimIndent()
        val response = Response.error<Any>(401, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.Unauthenticated, error)
    }

    @Test
    fun `given INTERNAL_SERVER_ERROR response when mapApiResponse then returns ReceiptScanServerError`() {
        val json = """
            {
              "success": false,
              "error": "INTERNAL_SERVER_ERROR",
              "message": "Error interno."
            }
        """.trimIndent()
        val response = Response.error<Any>(500, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.ReceiptScanServerError, error)
    }
}
