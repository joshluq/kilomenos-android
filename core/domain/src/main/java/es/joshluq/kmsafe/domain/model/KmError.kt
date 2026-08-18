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

    /** An unexpected or unhandled error occurred. */
    data object UnknownError : KmError
}
