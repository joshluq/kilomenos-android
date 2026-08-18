package es.joshluq.kmsafe.domain.model

/**
 * Pure domain model representing a User in the system.
 * Free from any external framework or persistence dependencies.
 */
data class User(
    val id: String,
    val email: String,
    val name: String
)
