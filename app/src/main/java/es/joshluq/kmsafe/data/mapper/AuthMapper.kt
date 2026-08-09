package es.joshluq.kmsafe.data.mapper

import es.joshluq.kmsafe.data.remote.model.EntitlementsModel
import es.joshluq.kmsafe.data.remote.model.UserSessionModel
import es.joshluq.kmsafe.data.remote.response.UserResponse
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
