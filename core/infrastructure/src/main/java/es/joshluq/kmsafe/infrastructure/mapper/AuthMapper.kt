package es.joshluq.kmsafe.infrastructure.mapper

import es.joshluq.authkit.session.model.SessionState
import es.joshluq.kmsafe.domain.model.AuthSessionState
import es.joshluq.kmsafe.infrastructure.remote.model.EntitlementsModel
import es.joshluq.kmsafe.infrastructure.remote.model.UserSessionModel
import es.joshluq.kmsafe.infrastructure.remote.response.UserResponse
import es.joshluq.kmsafe.domain.model.User

/**
 * Maps [UserResponse] from data layer to [User] domain model.
 */
fun UserResponse.toDomain(): User {
    val userEmail = checkNotNull(email) { "User email cannot be null" }
    return User(
        id = checkNotNull(id) { "User ID cannot be null" },
        email = userEmail,
        name = userMetadata?.name ?: userEmail.substringBefore("@")
            .replaceFirstChar { it.uppercase() }
    )
}

/**
 * Maps [User] domain model to [UserSessionModel] for persistence.
 */
fun User.toSessionModel(entitlements: EntitlementsModel? = null): UserSessionModel {
    return UserSessionModel(
        id = id,
        email = email,
        name = name,
        entitlements = entitlements
    )
}

/**
 * Maps [UserSessionModel] back to [User] domain model.
 */
fun UserSessionModel.toDomain(): User {
    return User(
        id = id,
        email = email,
        name = name
    )
}

/**
 * Maps [SessionState] from the AuthKit SDK to the domain-owned [AuthSessionState].
 *
 * This function acts as an **Anti-Corruption Layer (ACL)** between the infrastructure
 * kit and the domain Bounded Context. It is the single, explicit translation point
 * and must remain exclusively in the infrastructure layer.
 */
fun SessionState.toDomain(): AuthSessionState = when (this) {
    SessionState.Active -> AuthSessionState.Active
    SessionState.ExpiringSoon -> AuthSessionState.ExpiringSoon
    SessionState.Idle -> AuthSessionState.Idle
    SessionState.Initializing -> AuthSessionState.Initializing
}

