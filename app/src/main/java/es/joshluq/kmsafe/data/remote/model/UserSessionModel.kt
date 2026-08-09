package es.joshluq.kmsafe.data.remote.model

import es.joshluq.authkit.session.model.SessionData
import kotlinx.serialization.Serializable

/**
 * Data model used for session persistence in AuthKit.
 * Adheres to framework requirements while keeping the domain pure.
 */
@Serializable
data class UserSessionModel(
    val id: String,
    val email: String,
    val name: String,
    val entitlements: EntitlementsModel? = null
) : SessionData
