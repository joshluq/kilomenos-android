package es.joshluq.kmsafe.domain.model

/**
 * Sealed class representing all possible application-level errors.
 * This allows the UI to handle errors in a type-safe and localized manner.
 */
sealed interface KmError {
    /** The provided email or password is incorrect. */
    data object InvalidCredentials : KmError

    /** The email address is already in use by another account. */
    data object UserAlreadyRegistered : KmError

    /** The password does not meet the security requirements. */
    data object WeakPassword : KmError

    /** The provided email address has an invalid format. */
    data object InvalidEmail : KmError

    /** A problem occurred while communicating with the network. */
    data object NetworkError : KmError

    /** A server-side error occurred with a specific HTTP status code. */
    data class ServerError(val code: Int) : KmError

    /** The provided numeric values for a fuel expense (volume, price, total) are invalid. */
    data object InvalidFuelExpenseValues : KmError

    /** The fuel expenses feature is only available for Premium or Trial users. */
    data object FuelExpensesPremiumOnly : KmError

    /** The promotional trial period has already been used on this device. */
    data object TrialAlreadyUsed : KmError

    /** The requested fuel expense entry was not found. */
    data object ExpenseNotFound : KmError

    /** The monthly quota for AI receipt scanning has been exceeded. */
    data object ReceiptScanQuotaExceeded : KmError

    /** The uploaded file is not a valid receipt image or document. */
    data object InvalidReceiptImage : KmError

    /** The burst rate limit for AI receipt scans has been exceeded. */
    data class ReceiptScanRateLimitBurst(val retryAfterSeconds: Int) : KmError

    /** The daily rate limit (e.g. 20 scans) for AI receipt scans has been exceeded. */
    data object ReceiptScanRateLimitDaily : KmError

    /** Cooldown applied after consecutive invalid receipt uploads. */
    data class ReceiptScanRateLimitInvalidDocs(val retryAfterSeconds: Int) : KmError

    /** The uploaded image format is not supported by OCR. */
    data object UnsupportedImageFormat : KmError

    /** The user does not have permission to access the specified receipt file path. */
    data object UnauthorizedFileAccess : KmError

    /** Server-side OCR processing failure or temporary service unavailability. */
    data object ReceiptScanServerError : KmError

    /** User is not logged in or session has expired. */
    data object Unauthenticated : KmError

    /** The vehicle name provided is invalid or blank. */
    data object InvalidVehicleName : KmError

    /** The contract metrics (kms or months) are invalid. */
    data object InvalidContractMetrics : KmError

    /** Multi-vehicle fleet management is only available for Premium users. */
    data object MultiVehicleLimitReached : KmError

    // --- Official Backend Error Catalog (KILOMENOS-18) ---

    // A. Entitlements & Suscripción (Paywall)
    /** Resource or sync is exclusive to active subscribers (PREMIUM_REQUIRED / 403). */
    data object PremiumRequired : KmError

    /** Physical device has already reached the maximum trial activations (DEVICE_TRIAL_LIMIT / 400). */
    data object DeviceTrialLimit : KmError

    /** User attempted to start a trial while already having an active subscription (ALREADY_PREMIUM / 400). */
    data object AlreadyPremium : KmError

    /** Google Play purchase token is invalid or expired (PURCHASE_VERIFICATION_FAILED / 400 or 402). */
    data object PurchaseVerificationFailed : KmError

    // B. Autenticación y Tokens
    /** Session invalid or unauthorized (AUTH_UNAUTHORIZED / 401). */
    data object AuthUnauthorized : KmError

    /** JWT expired, requiring transparent refresh (AUTH_TOKEN_EXPIRED / 401). */
    data object AuthTokenExpired : KmError

    /** Supabase or backend API key missing or invalid (API_KEY_INVALID / 401). */
    data object ApiKeyInvalid : KmError

    // C. Validación de Peticiones
    /** Malformed JSON payload in request body (INVALID_JSON_BODY / 400). */
    data object InvalidJsonBody : KmError

    /** A mandatory parameter or field is missing (VALIDATION_REQUIRED_FIELD / 400). */
    data class ValidationRequiredField(val field: String) : KmError

    /** A field value has an invalid format (INVALID_FORMAT / 400). */
    data class InvalidFormat(val field: String, val reason: String? = null) : KmError

    // D. Recursos y Persistencia
    /** Notification not found or belongs to another user (NOTIFICATION_NOT_FOUND / 404). */
    data object NotificationNotFound : KmError

    /** Generic resource not found (NOT_FOUND / 404). */
    data object ResourceNotFound : KmError

    /** Duplicate record or version conflict (CONFLICT / 409). */
    data object Conflict : KmError

    // E. Sistema y Servicios Externos
    /** Internal database error in PostgreSQL or RLS (DATABASE_ERROR / 500). */
    data class DatabaseError(val code: Int = 500) : KmError

    /** Failed to dispatch push notification through FCM (FCM_DISPATCH_FAILED / 502). */
    data object FcmDispatchFailed : KmError

    /** Service is under maintenance or temporarily unavailable (SERVICE_UNAVAILABLE / 503). */
    data class ServiceUnavailable(val retryAfterSeconds: Int? = null) : KmError

    /** An unexpected or unhandled error occurred. */
    data object UnknownError : KmError
}
