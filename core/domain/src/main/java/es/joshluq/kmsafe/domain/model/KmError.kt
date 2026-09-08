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

    /** An unexpected or unhandled error occurred. */
    data object UnknownError : KmError
}
