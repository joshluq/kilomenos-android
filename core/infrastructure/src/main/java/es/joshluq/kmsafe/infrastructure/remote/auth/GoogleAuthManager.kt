package es.joshluq.kmsafe.infrastructure.remote.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.service.SocialAuthService
import es.joshluq.kmsafe.infrastructure.InfrastructureConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling Google Sign-In using the Credential Manager API.
 */
@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: InfrastructureConfig,
    private val logger: LoggerKit
) : SocialAuthService {
    /**
     * Initiates the Google Sign-In flow and returns the ID Token.
     */
    override suspend fun signIn(context: Any): String? {
        val platformContext = context as? Context ?: return null
        val credentialManager = CredentialManager.create(platformContext)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(config.googleWebClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                context = platformContext,
                request = request
            )
            handleSignInResult(result)
        } catch (e: Exception) {
            logger.e("GoogleAuthManager", "Sign-in failed", e)
            null
        }
    }

    /**
     * Clears the credential state in CredentialManager upon sign out.
     */
    override suspend fun signOut() {
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            logger.i("GoogleAuthManager", "Credential state cleared successfully")
        } catch (e: Exception) {
            logger.e("GoogleAuthManager", "Failed to clear credential state", e)
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse): String? {
        val credential = result.credential

        return when (credential.type) {
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    logger.i("GoogleAuthManager", "Sign-in success for: ${googleIdTokenCredential.displayName}")
                    googleIdTokenCredential.idToken
                } catch (e: Exception) {
                    logger.e("GoogleAuthManager", "Failed to parse Google ID Token credential", e)
                    null
                }
            }
            else -> {
                logger.e("GoogleAuthManager", "Unexpected credential type: ${credential.type}")
                null
            }
        }
    }
}
