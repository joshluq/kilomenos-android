package es.joshluq.kmsafe.data.mapper

import es.joshluq.kmsafe.data.remote.model.UserSessionModel
import es.joshluq.kmsafe.data.remote.response.UserResponse
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.User

/**
 * Maps [UserResponse] from data layer to [User] domain model.
 *
 * @param subscriptionLevel The level of subscription from the root response.
 */
fun UserResponse.toDomain(subscriptionLevel: String?): User {
    val userEmail = checkNotNull(email) { "User email cannot be null" }
    val level = when (subscriptionLevel?.uppercase()) {
        "PREMIUM" -> SubscriptionLevel.PREMIUM
        else -> SubscriptionLevel.FREE
    }
    return User(
        id = checkNotNull(id) { "User ID cannot be null" },
        email = userEmail,
        name = userMetadata?.name ?: userEmail.substringBefore("@")
            .replaceFirstChar { it.uppercase() },
        subscriptionLevel = level
    )
}

/**
 * Maps [User] domain model to [UserSessionModel] for persistence.
 */
fun User.toSessionModel(): UserSessionModel {
    return UserSessionModel(
        id = id,
        email = email,
        name = name,
        subscriptionLevel = subscriptionLevel.name
    )
}

/**
 * Maps [UserSessionModel] back to [User] domain model.
 */
fun UserSessionModel.toDomain(): User {
    val level = runCatching {
        SubscriptionLevel.valueOf(subscriptionLevel)
    }.getOrDefault(SubscriptionLevel.FREE)

    return User(
        id = id,
        email = email,
        name = name,
        subscriptionLevel = level
    )
}
