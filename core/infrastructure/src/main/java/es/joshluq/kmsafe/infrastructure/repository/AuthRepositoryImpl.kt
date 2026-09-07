package es.joshluq.kmsafe.infrastructure.repository

import es.joshluq.analyticskit.domain.model.AnalyticsEvent
import es.joshluq.analyticskit.sdk.AnalyticskitManager
import es.joshluq.authkit.session.model.Token
import es.joshluq.authkit.session.model.TokenHolder
import es.joshluq.foundationkit.coroutines.DispatcherProvider
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.infrastructure.local.AppDatabase
import es.joshluq.kmsafe.infrastructure.local.datasource.PreferencesDataSource
import es.joshluq.kmsafe.infrastructure.mapper.ErrorMapper
import es.joshluq.kmsafe.infrastructure.mapper.toDomain
import es.joshluq.kmsafe.infrastructure.mapper.toModel
import es.joshluq.kmsafe.infrastructure.mapper.toSessionModel
import es.joshluq.kmsafe.infrastructure.remote.api.AuthApiService
import es.joshluq.kmsafe.infrastructure.remote.api.AuthenticatedAuthApiService
import es.joshluq.kmsafe.infrastructure.remote.auth.UserSessionDataSource
import es.joshluq.kmsafe.infrastructure.remote.request.OAuthSignInRequest
import es.joshluq.kmsafe.infrastructure.remote.request.SignInRequest
import es.joshluq.kmsafe.infrastructure.remote.request.SignUpRequest
import es.joshluq.kmsafe.infrastructure.remote.request.UpdateSubscriptionRequest
import es.joshluq.kmsafe.domain.model.AuthSessionState
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.model.KmError
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.domain.model.SubscriptionLevel
import es.joshluq.kmsafe.domain.model.User
import es.joshluq.kmsafe.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of [AuthRepository] using [AuthApiService] and [UserSessionDataSource].
 */
class AuthRepositoryImpl @Inject constructor(
    private val apiService: AuthApiService,
    private val authenticatedApiService: AuthenticatedAuthApiService,
    private val sessionDataSource: UserSessionDataSource,
    private val appDatabase: AppDatabase,
    private val preferencesDataSource: PreferencesDataSource,
    private val errorMapper: ErrorMapper,
    private val logger: LoggerKit,
    private val dispatchers: DispatcherProvider,
    private val analytics: AnalyticskitManager
) : AuthRepository {

    override fun signIn(email: String, password: String): Flow<User> = flow {
        logger.d("AuthRepository", "Starting sign in for email: $email")
        val request = SignInRequest(email = email, password = password)
        val response = apiService.signIn(request)

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.success && body.user != null && body.session != null) {
                logger.i("AuthRepository", "Sign in success for user: ${body.user.id}. Starting session.")

                val tokens = TokenHolder.withTokens(
                    Token.Access(body.session.accessToken ?: ""),
                    Token.Refresh(body.session.refreshToken ?: "")
                )
                val user = body.user.toDomain()

                // Seed initial entitlements from login response
                val initialLevelStr = body.subscriptionLevel?.uppercase()
                logger.i("AuthRepository", "Sign in success. User: ${user.email}, Level: $initialLevelStr")
                
                val initialLevel = when (initialLevelStr) {
                    "PREMIUM" -> SubscriptionLevel.PREMIUM
                    "TRIAL" -> SubscriptionLevel.TRIAL
                    else -> SubscriptionLevel.FREE
                }
                val initialEntitlements = Entitlements.Default.copy(subscriptionLevel = initialLevel)

                sessionDataSource.startSession(tokens)
                val sessionData = user.toSessionModel(initialEntitlements.toModel())
                logger.d("AuthRepository", "Saving session data post-login: $sessionData")
                sessionDataSource.saveSessionData(sessionData)

                // Double check session persistence immediately
                val verifiedSession = sessionDataSource.getCurrentUserSession()
                logger.d("AuthRepository", "Verified session data immediately: $verifiedSession")
                
                analytics.track(
                    AnalyticsEvent.Custom("login_success", mapOf("user_id" to user.id, "method" to "credentials"))
                )

                emit(user)
            } else {
                val errorMsg = body?.error ?: "Authentication failed"
                logger.e("AuthRepository", "Sign in failed with body error: $errorMsg")
                throw KmException(errorMapper.mapApiResponse(response, body?.error))
            }
        } else {
            logger.e("AuthRepository", "Sign in failed with HTTP error: ${response.code()}")
            throw KmException(errorMapper.mapApiResponse(response))
        }
    }.flowOn(dispatchers.io)

    override fun signInWithGoogle(idToken: String): Flow<User> = flow {
        logger.d("AuthRepository", "Starting Google OAuth sign in")
        val request = OAuthSignInRequest(provider = "google", token = idToken)
        val response = apiService.signInWithOAuth(request)

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.success && body.user != null && body.session != null) {
                logger.i("AuthRepository", "Google sign in success. Starting session.")

                val tokens = TokenHolder.withTokens(
                    Token.Access(body.session.accessToken ?: ""),
                    Token.Refresh(body.session.refreshToken ?: "")
                )
                val user = body.user.toDomain()

                // Seed initial entitlements from login response
                val initialLevelStr = body.subscriptionLevel?.uppercase()
                logger.i("AuthRepository", "Google sign in success. Level: $initialLevelStr")

                val initialLevel = when (initialLevelStr) {
                    "PREMIUM" -> SubscriptionLevel.PREMIUM
                    "TRIAL" -> SubscriptionLevel.TRIAL
                    else -> SubscriptionLevel.FREE
                }
                val initialEntitlements = Entitlements.Default.copy(subscriptionLevel = initialLevel)

                sessionDataSource.startSession(tokens)
                val sessionData = user.toSessionModel(initialEntitlements.toModel())
                logger.d("AuthRepository", "Saving Google session data: $sessionData")
                sessionDataSource.saveSessionData(sessionData)

                // Double check
                val verifiedSession = sessionDataSource.getCurrentUserSession()
                logger.d("AuthRepository", "Verified Google session: $verifiedSession")

                analytics.track(
                    AnalyticsEvent.Custom("login_success", mapOf("user_id" to user.id, "method" to "google"))
                )

                emit(user)
            } else {
                val errorMsg = body?.error ?: "OAuth authentication failed"
                logger.e("AuthRepository", "Google sign in failed with body error: $errorMsg")
                throw KmException(errorMapper.mapApiResponse(response, body?.error))
            }
        } else {
            logger.e("AuthRepository", "Google sign in failed with HTTP error: ${response.code()}")
            throw KmException(errorMapper.mapApiResponse(response))
        }
    }.flowOn(dispatchers.io)

    override fun signUp(email: String, password: String, name: String?): Flow<User> = flow {
        logger.d("AuthRepository", "Starting sign up for email: $email")
        val request = SignUpRequest(email = email, password = password, name = name)
        val response = apiService.signUp(request)

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.success && body.user != null) {
                logger.i("AuthRepository", "Sign up success for email: $email")

                if (body.session != null) {
                    val tokens = TokenHolder.withTokens(
                        Token.Access(body.session.accessToken ?: ""),
                        Token.Refresh(body.session.refreshToken ?: "")
                    )
                    val user = body.user.toDomain()

                    // Seed initial entitlements
                    val initialLevel = when (body.subscriptionLevel.uppercase()) {
                        "PREMIUM" -> SubscriptionLevel.PREMIUM
                        "TRIAL" -> SubscriptionLevel.TRIAL
                        else -> SubscriptionLevel.FREE
                    }
                    val initialEntitlements = Entitlements.Default.copy(subscriptionLevel = initialLevel)

                    sessionDataSource.startSession(tokens)
                    sessionDataSource.saveSessionData(user.toSessionModel(initialEntitlements.toModel()))

                    analytics.track(AnalyticsEvent.Custom("signup_success", mapOf("user_id" to user.id)))

                    emit(user)
                } else {
                    emit(body.user.toDomain())
                }
            } else {
                val errorMsg = body?.error ?: "Sign up failed"
                logger.e("AuthRepository", "Sign up failed with body error: $errorMsg")
                throw KmException(errorMapper.mapApiResponse(response, body?.error))
            }
        } else {
            val errorMsg = runCatching {
                val errorJson = response.errorBody()?.string()
                if (errorJson?.contains("\"error\"") != true) {
                    throw Exception("Invalid JSON format")
                }
                errorJson.substringAfter("\"error\":\"").substringBefore("\"")
            }.getOrNull()

            logger.e("AuthRepository", "Sign up failed with HTTP error: ${response.code()}, message: $errorMsg")
            throw KmException(errorMapper.mapApiResponse(response, errorMsg))
        }
    }.flowOn(dispatchers.io)

    override fun updateSubscription(level: SubscriptionLevel): Flow<User> = flow {
        logger.d("AuthRepository", "Updating subscription to: ${level.name}")
        val request = UpdateSubscriptionRequest(level = level.name)
        val response = authenticatedApiService.updateSubscription(request)

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.success) {
                logger.i("AuthRepository", "Subscription update success. Level: ${body.subscriptionLevel}")

                // Trigger an entitlements update to sync the new status
                val entitlements = getEntitlements().first()
                updateEntitlements(
                    entitlements.copy(
                        subscriptionLevel = level
                    )
                )

                val currentUser = sessionDataSource.getCurrentUserSession()?.toDomain()
                if (currentUser != null) {
                    emit(currentUser)
                } else {
                    throw KmException(KmError.UnknownError)
                }
            } else {
                throw KmException(errorMapper.mapApiResponse(response, body?.message))
            }
        } else {
            throw KmException(errorMapper.mapApiResponse(response))
        }
    }.flowOn(dispatchers.io)

    override fun getSessionState(): Flow<AuthSessionState> {
        return sessionDataSource.getSessionState()
    }

    override fun getCurrentUser(): Flow<User?> {
        return sessionDataSource.observeUserSession().map { session ->
            val user = session?.toDomain()
            logger.d("AuthRepository", "getCurrentUser observed session: $session -> domain user: $user")
            user
        }
    }

    override fun getEntitlements(): Flow<Entitlements> {
        return sessionDataSource.observeUserSession().map { session ->
            val entitlements = session?.entitlements?.toDomain() ?: Entitlements.Default
            logger.d("AuthRepository", "getEntitlements observed session: $session -> entitlements: $entitlements")
            entitlements
        }
    }

    override suspend fun updateEntitlements(entitlements: Entitlements) {
        val currentSession = sessionDataSource.getCurrentUserSession()
        if (currentSession != null) {
            val updatedSession = currentSession.copy(entitlements = entitlements.toModel())
            sessionDataSource.saveSessionData(updatedSession)

            // Sync analytics
            analytics.addGlobalProperty("subscription_level", entitlements.subscriptionLevel.name)
        }
    }

    override fun signOut(clearLocalData: Boolean): Flow<Unit> = flow {
        logger.i("AuthRepository", "Ending session. clearLocalData: $clearLocalData")
        analytics.track(AnalyticsEvent.Custom("logout"))
        analytics.removeGlobalProperty("subscription_level")
        sessionDataSource.endSession()
        if (clearLocalData) {
            appDatabase.clearAllTables()
            preferencesDataSource.clearAllPreferences()
        }
        emit(Unit)
    }.flowOn(dispatchers.io)

    override fun deleteAccount(): Flow<Unit> = flow {
        logger.i("AuthRepository", "Initiating account deletion")
        val response = authenticatedApiService.deleteAccount()
        if (response.isSuccessful) {
            logger.i("AuthRepository", "Account deleted from server successfully")
            sessionDataSource.endSession()
            appDatabase.clearAllTables()
            preferencesDataSource.clearAllPreferences()
            analytics.track(AnalyticsEvent.Custom("account_deleted"))
            emit(Unit)
        } else {
            logger.e("AuthRepository", "Failed to delete account. Code: ${response.code()}")
            throw KmException(errorMapper.mapApiResponse(response))
        }
    }.flowOn(dispatchers.io)
}
