package es.joshluq.kmsafe.domain.service

/**
 * Service for handling authentication with social providers.
 */
interface SocialAuthService {
    /**
     * Initiates the sign-in flow with a social provider.
     *
     * @param context Platform-specific context needed for the UI flow.
     * @return The ID Token if successful, null otherwise.
     */
    suspend fun signIn(context: Any): String?
}
