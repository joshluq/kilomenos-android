package es.joshluq.kmsafe.domain.validator

import javax.inject.Inject

/**
 * Validator for email addresses using pure Kotlin/Regex.
 */
class EmailValidator @Inject constructor() : Validator<String> {
    private val emailRegex = Regex(
        "[a-zA-Z0-9+._%\\-]{1,256}" +
            "@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+"
    )

    /**
     * Checks if the given [value] is valid.
     */
    override fun isValid(value: String): Boolean {
        return value.isNotBlank() && emailRegex.matches(value)
    }
}
