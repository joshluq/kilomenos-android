package es.joshluq.kmsafe.domain.validator

import javax.inject.Inject

/**
 * Validator for passwords.
 * Rules:
 * - At least 8 characters
 * - At least one uppercase letter
 * - At least one lowercase letter
 * - At least one number
 * - At least one special character (@$!%*?&)
 */
class PasswordValidator @Inject constructor() : Validator<String> {

    private val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")

    /**
     * Checks if the given [value] is valid.
     */
    override fun isValid(value: String): Boolean {
        return value.isNotBlank() && passwordRegex.matches(value)
    }
}
