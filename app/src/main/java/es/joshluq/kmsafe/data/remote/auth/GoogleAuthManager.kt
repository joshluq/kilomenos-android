package es.joshluq.kmsafe.data.remote.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling Google Sign-In using the Credential Manager API.
 */
@Singleton
class GoogleAuthManager @Inject constructor(
    private val logger: LoggerKit
) {
    /**
     * Initiates the Google Sign-In flow and returns the ID Token.
     */
    suspend fun signIn(context: Context): String? {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )
            handleSignInResult(result)
        } catch (e: Exception) {
            logger.e("GoogleAuthManager", "Sign-in failed", e)
            null
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
