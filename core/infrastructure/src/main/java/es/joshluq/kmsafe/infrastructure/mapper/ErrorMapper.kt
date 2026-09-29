package es.joshluq.kmsafe.infrastructure.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import es.joshluq.kmsafe.domain.model.KmError
import retrofit2.Response
import javax.inject.Inject

/**
 * Mapper responsible for translating data layer errors (Retrofit responses and exceptions)
 * into domain-level [KmError] types based on the official backend error catalog (KILOMENOS-18).
 */
class ErrorMapper @Inject constructor() {

    private val jsonMapper = ObjectMapper()

    /**
     * Maps a Retrofit [Response] failure or body error message to a [KmError].
     */
    fun mapApiResponse(response: Response<*>, bodyError: String? = null): KmError {
        val rawJson = bodyError ?: response.errorBody()?.string().orEmpty()
        val rootNode = try {
            if (rawJson.isNotBlank()) jsonMapper.readTree(rawJson) else null
        } catch (_: Exception) {
            null
        }
        val errorNode = rootNode?.get("error")

        // Extract canonical error code
        val code = when {
            rootNode?.get("code")?.isTextual == true -> rootNode.get("code").asText()
            errorNode?.isObject == true && errorNode.get("code")?.isTextual == true -> errorNode.get("code").asText()
            errorNode?.isTextual == true -> errorNode.asText()
            rootNode?.get("error_code")?.isTextual == true -> rootNode.get("error_code").asText()
            else -> null
        }?.trim()?.uppercase()

        // Extract validation field
        val field = when {
            rootNode?.get("field")?.isTextual == true -> rootNode.get("field").asText()
            errorNode?.isObject == true && errorNode.get("field")?.isTextual == true -> errorNode.get("field").asText()
            else -> ""
        }

        // Extract retry after seconds
        val retryAfter = when {
            rootNode?.has("retry_after_seconds") == true -> rootNode.get("retry_after_seconds")?.asInt()
            errorNode?.isObject == true && errorNode.has("retry_after_seconds") == true -> errorNode.get("retry_after_seconds")?.asInt()
            else -> null
        } ?: response.headers().get("Retry-After")?.toIntOrNull() ?: 60

        // 1. Primary Mapping: Canonical Backend Error Codes
        return when (code) {
            // A. Entitlements & Suscripción (Paywall)
            "PREMIUM_REQUIRED" -> KmError.PremiumRequired
            "TRIAL_ALREADY_USED" -> KmError.TrialAlreadyUsed
            "DEVICE_TRIAL_LIMIT" -> KmError.DeviceTrialLimit
            "ALREADY_PREMIUM" -> KmError.AlreadyPremium
            "PURCHASE_VERIFICATION_FAILED" -> KmError.PurchaseVerificationFailed

            // B. Autenticación y Tokens
            "AUTH_UNAUTHORIZED" -> KmError.AuthUnauthorized
            "AUTH_TOKEN_EXPIRED" -> KmError.AuthTokenExpired
            "AUTH_INVALID_CREDENTIALS" -> KmError.InvalidCredentials
            "API_KEY_INVALID" -> KmError.ApiKeyInvalid

            // C. Validación de Peticiones
            "INVALID_JSON_BODY" -> KmError.InvalidJsonBody
            "VALIDATION_REQUIRED_FIELD" -> KmError.ValidationRequiredField(field)
            "INVALID_FORMAT" -> KmError.InvalidFormat(field)

            // D. Recursos y Persistencia
            "NOTIFICATION_NOT_FOUND" -> KmError.NotificationNotFound
            "NOT_FOUND" -> KmError.ResourceNotFound
            "CONFLICT" -> KmError.Conflict

            // E. Sistema y Servicios Externos
            "DATABASE_ERROR" -> KmError.DatabaseError(response.code())
            "FCM_DISPATCH_FAILED" -> KmError.FcmDispatchFailed
            "INTERNAL_SERVER_ERROR" -> KmError.ReceiptScanServerError
            "SERVICE_UNAVAILABLE" -> KmError.ServiceUnavailable(retryAfter)

            // OCR & Receipt AI Scan Legacy Codes
            "RATE_LIMIT_BURST_EXCEEDED" -> KmError.ReceiptScanRateLimitBurst(retryAfter)
            "RATE_LIMIT_DAILY_EXCEEDED" -> KmError.ReceiptScanRateLimitDaily
            "RATE_LIMIT_INVALID_DOCS_COOLDOWN" -> KmError.ReceiptScanRateLimitInvalidDocs(retryAfter)
            "PREMIUM_FEATURE_REQUIRED" -> KmError.FuelExpensesPremiumOnly
            "DOCUMENT_NOT_A_FUEL_RECEIPT" -> KmError.InvalidReceiptImage
            "UNSUPPORTED_MEDIA_TYPE" -> KmError.UnsupportedImageFormat
            "UNAUTHORIZED_FILE_ACCESS" -> KmError.UnauthorizedFileAccess
            "INVALID_TOKEN" -> KmError.Unauthenticated

            // 2. Secondary Heuristics Fallback (Natural Language matching)
            else -> when {
                rawJson.contains("usuarios PREMIUM", ignoreCase = true) -> KmError.FuelExpensesPremiumOnly
                rawJson.contains("Invalid login credentials", ignoreCase = true) -> KmError.InvalidCredentials
                rawJson.contains("User already registered", ignoreCase = true) -> KmError.UserAlreadyRegistered
                rawJson.contains("password", ignoreCase = true) && rawJson.contains("characters", ignoreCase = true) -> KmError.WeakPassword
                rawJson.contains("email", ignoreCase = true) && rawJson.contains("format", ignoreCase = true) -> KmError.InvalidEmail
                rawJson.contains("periodo de prueba", ignoreCase = true) -> KmError.TrialAlreadyUsed
                rawJson.contains("no encontrado", ignoreCase = true) && rawJson.contains("Gasto", ignoreCase = true) -> KmError.ExpenseNotFound
                rawJson.contains("UNAUTHORIZED", ignoreCase = true) && response.code() == 401 -> KmError.Unauthenticated

                // 3. HTTP Status Fallbacks
                response.code() == 401 -> KmError.Unauthenticated
                response.code() == 403 -> KmError.PremiumRequired
                response.code() == 404 -> KmError.ResourceNotFound
                response.code() == 409 -> KmError.Conflict
                response.code() == 503 -> KmError.ServiceUnavailable(retryAfter)
                response.code() >= 500 -> KmError.ServerError(response.code())
                else -> KmError.UnknownError
            }
        }
    }
}
