package es.joshluq.kmsafe.domain.validator

/**
 * Base interface for data validation.
 * Adheres to Dependency Inversion Principle.
 *
 * @param T The type of data to validate.
 */
interface Validator<in T> {
    /**
     * Checks if the given [value] is valid according to specific rules.
     */
    fun isValid(value: T): Boolean
}
