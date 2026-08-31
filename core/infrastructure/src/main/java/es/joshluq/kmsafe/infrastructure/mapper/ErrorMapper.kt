package es.joshluq.kmsafe.infrastructure.mapper

import es.joshluq.kmsafe.domain.model.KmError
import retrofit2.Response
import javax.inject.Inject

/**
 * Mapper responsible for translating data layer errors (Retrofit responses and exceptions)
 * into domain-level [KmError] types.
 */
class ErrorMapper @Inject constructor() {

    /**
     * Maps a Retrofit [Response] failure or body error message to a [KmError].
     */
    fun mapApiResponse(response: Response<*>, bodyError: String? = null): KmError {
        val errorMsg = bodyError ?: response.errorBody()?.string() ?: ""

        return when {
            errorMsg.contains("Invalid login credentials", ignoreCase = true) -> KmError.InvalidCredentials
            errorMsg.contains("User already registered", ignoreCase = true) -> KmError.UserAlreadyRegistered
            errorMsg.contains("password", ignoreCase = true) && errorMsg.contains("characters", ignoreCase = true) -> KmError.WeakPassword
            errorMsg.contains("email", ignoreCase = true) && errorMsg.contains("format", ignoreCase = true) -> KmError.InvalidEmail
            errorMsg.contains("usuarios PREMIUM", ignoreCase = true) -> KmError.FuelExpensesPremiumOnly
            errorMsg.contains("periodo de prueba", ignoreCase = true) -> KmError.TrialAlreadyUsed
            errorMsg.contains("no encontrado", ignoreCase = true) && errorMsg.contains("Gasto", ignoreCase = true) -> KmError.ExpenseNotFound
            response.code() >= 500 -> KmError.ServerError(response.code())
            else -> KmError.UnknownError
        }
    }
}
