package es.joshluq.kmsafe.domain.repository

import es.joshluq.kmsafe.domain.model.AuthSessionState
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Authentication operations.
 */
interface AuthRepository {
    /**
     * Returns a flow of the current user entitlements.
     */
    fun getEntitlements(): Flow<Entitlements>

    /**
     * Updates the user's entitlements in the current session.
     */
    suspend fun updateEntitlements(entitlements: Entitlements)

    /**
     * Attempts to sign in a user with [email] and [password].
     */
    fun signIn(email: String, password: String): Flow<User>

    /**
     * Attempts to sign in a user with Google [idToken].
     */
    fun signInWithGoogle(idToken: String): Flow<User>

    /**
     * Attempts to register a new user with [email], [password] and [name].
     */
    fun signUp(email: String, password: String, name: String?): Flow<User>

    /**
     * Returns a flow of the current session state.
     */
    fun getSessionState(): Flow<AuthSessionState>

    /**
     * Returns a flow of the current authenticated user data.
     */
    fun getCurrentUser(): Flow<User?>

    /**
     * Updates the user's subscription level.
     */
    fun updateSubscription(level: SubscriptionLevel): Flow<User>

    /**
     * Ends the current session and optionally clears all local authentication data.
     *
     * @param clearLocalData Whether to wipe the local database.
     */
    fun signOut(clearLocalData: Boolean = true): Flow<Unit>

    /**
     * Permanently deletes the user account from the server and local storage.
     */
    fun deleteAccount(): Flow<Unit>
}
