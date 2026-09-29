package es.joshluq.kmsafe.infrastructure.mapper

import es.joshluq.kmsafe.domain.model.KmError
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ErrorMapperTest {

    private val mapper = ErrorMapper()

    // =========================================================================
    // Category A: Entitlements & Suscripción (Paywall) - KILOMENOS-18
    // =========================================================================

    @Test
    fun `given PREMIUM_REQUIRED response when mapApiResponse then returns PremiumRequired`() {
        val json = """
            {
              "success": false,
              "error": {
                "code": "PREMIUM_REQUIRED",
                "message": "Recurso o sync exclusivo para suscriptores."
              }
            }
        """.trimIndent()
        val response = Response.error<Any>(403, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.PremiumRequired, error)
    }

    @Test
    fun `given TRIAL_ALREADY_USED response when mapApiResponse then returns TrialAlreadyUsed`() {
        val json = """
            {
              "success": false,
              "code": "TRIAL_ALREADY_USED",
              "message": "La cuenta ya consumió su período de prueba de 7 días."
            }
        """.trimIndent()
        val response = Response.error<Any>(400, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.TrialAlreadyUsed, error)
    }

    @Test
    fun `given DEVICE_TRIAL_LIMIT response when mapApiResponse then returns DeviceTrialLimit`() {
        val json = """
            {
              "success": false,
              "code": "DEVICE_TRIAL_LIMIT",
              "message": "El dispositivo físico ya fue utilizado para un trial previo."
            }
        """.trimIndent()
        val response = Response.error<Any>(400, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.DeviceTrialLimit, error)
    }

    @Test
    fun `given ALREADY_PREMIUM response when mapApiResponse then returns AlreadyPremium`() {
        val json = """
            {
              "success": false,
              "error": "ALREADY_PREMIUM",
              "message": "Intento de iniciar trial teniendo suscripción activa."
            }
        """.trimIndent()
        val response = Response.error<Any>(400, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.AlreadyPremium, error)
    }

    @Test
    fun `given PURCHASE_VERIFICATION_FAILED response when mapApiResponse then returns PurchaseVerificationFailed`() {
        val json = """
            {
              "success": false,
              "error": {
                "code": "PURCHASE_VERIFICATION_FAILED",
                "message": "Token de Google Play no válido o expirado."
              }
            }
        """.trimIndent()
        val response = Response.error<Any>(400, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.PurchaseVerificationFailed, error)
    }

    // =========================================================================
    // Category B: Autenticación y Tokens - KILOMENOS-18
    // =========================================================================

    @Test
    fun `given AUTH_UNAUTHORIZED response when mapApiResponse then returns AuthUnauthorized`() {
        val json = """
            {
              "success": false,
              "code": "AUTH_UNAUTHORIZED",
              "message": "Sesión no iniciada o token inválido."
            }
        """.trimIndent()
        val response = Response.error<Any>(401, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.AuthUnauthorized, error)
    }

    @Test
    fun `given AUTH_TOKEN_EXPIRED response when mapApiResponse then returns AuthTokenExpired`() {
        val json = """
            {
              "success": false,
              "error": {
                "code": "AUTH_TOKEN_EXPIRED",
                "message": "JWT expirado."
              }
            }
        """.trimIndent()
        val response = Response.error<Any>(401, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.AuthTokenExpired, error)
    }

    @Test
    fun `given AUTH_INVALID_CREDENTIALS response when mapApiResponse then returns InvalidCredentials`() {
        val json = """
            {
              "success": false,
              "code": "AUTH_INVALID_CREDENTIALS",
              "message": "Contraseña o email incorrecto."
            }
        """.trimIndent()
        val response = Response.error<Any>(401, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.InvalidCredentials, error)
    }

    @Test
    fun `given API_KEY_INVALID response when mapApiResponse then returns ApiKeyInvalid`() {
        val json = """
            {
              "success": false,
              "code": "API_KEY_INVALID",
              "message": "Apikey de Supabase no suministrada o inválida."
            }
        """.trimIndent()
        val response = Response.error<Any>(401, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.ApiKeyInvalid, error)
    }

    // =========================================================================
    // Category C: Validación de Peticiones - KILOMENOS-18
    // =========================================================================

    @Test
    fun `given INVALID_JSON_BODY response when mapApiResponse then returns InvalidJsonBody`() {
        val json = """
            {
              "success": false,
              "code": "INVALID_JSON_BODY",
              "message": "JSON malformado en la petición."
            }
        """.trimIndent()
        val response = Response.error<Any>(400, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.InvalidJsonBody, error)
    }

    @Test
    fun `given VALIDATION_REQUIRED_FIELD response when mapApiResponse then returns ValidationRequiredField with field name`() {
        val json = """
            {
              "success": false,
              "error": {
                "code": "VALIDATION_REQUIRED_FIELD",
                "field": "fcm_token",
                "message": "Falta un parámetro mandatorio."
              }
            }
        """.trimIndent()
        val response = Response.error<Any>(400, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertTrue(error is KmError.ValidationRequiredField)
        assertEquals("fcm_token", (error as KmError.ValidationRequiredField).field)
    }

    @Test
    fun `given INVALID_FORMAT response when mapApiResponse then returns InvalidFormat with field name`() {
        val json = """
            {
              "success": false,
              "code": "INVALID_FORMAT",
              "field": "email",
              "message": "Formato incorrecto."
            }
        """.trimIndent()
        val response = Response.error<Any>(400, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertTrue(error is KmError.InvalidFormat)
        assertEquals("email", (error as KmError.InvalidFormat).field)
    }

    // =========================================================================
    // Category D: Recursos y Persistencia - KILOMENOS-18
    // =========================================================================

    @Test
    fun `given NOTIFICATION_NOT_FOUND response when mapApiResponse then returns NotificationNotFound`() {
        val json = """
            {
              "success": false,
              "code": "NOTIFICATION_NOT_FOUND",
              "message": "Notificación no encontrada o no pertenece al usuario."
            }
        """.trimIndent()
        val response = Response.error<Any>(404, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.NotificationNotFound, error)
    }

    @Test
    fun `given NOT_FOUND response when mapApiResponse then returns ResourceNotFound`() {
        val json = """
            {
              "success": false,
              "code": "NOT_FOUND",
              "message": "Recurso no existe."
            }
        """.trimIndent()
        val response = Response.error<Any>(404, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.ResourceNotFound, error)
    }

    @Test
    fun `given CONFLICT response when mapApiResponse then returns Conflict`() {
        val json = """
            {
              "success": false,
              "code": "CONFLICT",
              "message": "Registro duplicado o conflicto de versión."
            }
        """.trimIndent()
        val response = Response.error<Any>(409, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.Conflict, error)
    }

    // =========================================================================
    // Category E: Sistema y Servicios Externos - KILOMENOS-18
    // =========================================================================

    @Test
    fun `given DATABASE_ERROR response when mapApiResponse then returns DatabaseError`() {
        val json = """
            {
              "success": false,
              "code": "DATABASE_ERROR",
              "message": "Error interno en PostgreSQL o RLS."
            }
        """.trimIndent()
        val response = Response.error<Any>(500, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertTrue(error is KmError.DatabaseError)
        assertEquals(500, (error as KmError.DatabaseError).code)
    }

    @Test
    fun `given FCM_DISPATCH_FAILED response when mapApiResponse then returns FcmDispatchFailed`() {
        val json = """
            {
              "success": false,
              "code": "FCM_DISPATCH_FAILED",
              "message": "Fallo de conexión con Firebase Cloud Messaging."
            }
        """.trimIndent()
        val response = Response.error<Any>(502, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertEquals(KmError.FcmDispatchFailed, error)
    }

    @Test
    fun `given SERVICE_UNAVAILABLE response when mapApiResponse then returns ServiceUnavailable with retry seconds`() {
        val json = """
            {
              "success": false,
              "code": "SERVICE_UNAVAILABLE",
              "message": "Servicio en mantenimiento.",
              "retry_after_seconds": 120
            }
        """.trimIndent()
        val response = Response.error<Any>(503, json.toResponseBody())

        val error = mapper.mapApiResponse(response)

        assertTrue(error is KmError.ServiceUnavailable)
        assertEquals(120, (error as KmError.ServiceUnavailable).retryAfterSeconds)
    }

    // =========================================================================
    // Legacy OCR & AI Receipt Scan Scenarios
    // =========================================================================

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
              "message": "El escaneo de tickets mediante IA es una funcionalidad exclusiva de KiloMenos Premium."
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
