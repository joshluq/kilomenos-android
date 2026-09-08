package es.joshluq.kmsafe.infrastructure.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import es.joshluq.kmsafe.domain.model.KmError
import retrofit2.Response
import javax.inject.Inject

/**
 * Mapper responsible for translating data layer errors (Retrofit responses and exceptions)
 * into domain-level [KmError] types.
 */
class ErrorMapper @Inject constructor() {

    private val jsonMapper = ObjectMapper()

    /**
     * Maps a Retrofit [Response] failure or body error message to a [KmError].
     */
    fun mapApiResponse(response: Response<*>, bodyError: String? = null): KmError {
        val errorMsg = bodyError ?: response.errorBody()?.string() ?: ""

        val retryAfter = try {
            val node = jsonMapper.readTree(errorMsg)
            node.get("retry_after_seconds")?.asInt()
        } catch (_: Exception) {
            null
        } ?: response.headers().get("Retry-After")?.toIntOrNull() ?: 60

        return when {
            errorMsg.contains("RATE_LIMIT_BURST_EXCEEDED", ignoreCase = true) ->
                KmError.ReceiptScanRateLimitBurst(retryAfter)

            errorMsg.contains("RATE_LIMIT_DAILY_EXCEEDED", ignoreCase = true) ->
                KmError.ReceiptScanRateLimitDaily

            errorMsg.contains("RATE_LIMIT_INVALID_DOCS_COOLDOWN", ignoreCase = true) ->
                KmError.ReceiptScanRateLimitInvalidDocs(retryAfter)

            errorMsg.contains("usuarios PREMIUM", ignoreCase = true) ||
            errorMsg.contains("PREMIUM_FEATURE_REQUIRED", ignoreCase = true) ->
                KmError.FuelExpensesPremiumOnly

            errorMsg.contains("DOCUMENT_NOT_A_FUEL_RECEIPT", ignoreCase = true) ->
                KmError.InvalidReceiptImage

            errorMsg.contains("UNSUPPORTED_MEDIA_TYPE", ignoreCase = true) ->
                KmError.UnsupportedImageFormat

            errorMsg.contains("UNAUTHORIZED_FILE_ACCESS", ignoreCase = true) ->
                KmError.UnauthorizedFileAccess

            errorMsg.contains("INVALID_TOKEN", ignoreCase = true) ||
            (response.code() == 401 && errorMsg.contains("UNAUTHORIZED", ignoreCase = true)) ->
                KmError.Unauthenticated

            errorMsg.contains("INTERNAL_SERVER_ERROR", ignoreCase = true) ->
                KmError.ReceiptScanServerError

            errorMsg.contains("Invalid login credentials", ignoreCase = true) -> KmError.InvalidCredentials
            errorMsg.contains("User already registered", ignoreCase = true) -> KmError.UserAlreadyRegistered
            errorMsg.contains("password", ignoreCase = true) && errorMsg.contains("characters", ignoreCase = true) -> KmError.WeakPassword
            errorMsg.contains("email", ignoreCase = true) && errorMsg.contains("format", ignoreCase = true) -> KmError.InvalidEmail
            errorMsg.contains("periodo de prueba", ignoreCase = true) -> KmError.TrialAlreadyUsed
            errorMsg.contains("no encontrado", ignoreCase = true) && errorMsg.contains("Gasto", ignoreCase = true) -> KmError.ExpenseNotFound
            response.code() >= 500 -> KmError.ServerError(response.code())
            else -> KmError.UnknownError
        }
    }
}
