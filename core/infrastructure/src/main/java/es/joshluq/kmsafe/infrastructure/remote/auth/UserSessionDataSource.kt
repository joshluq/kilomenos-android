package es.joshluq.kmsafe.infrastructure.remote.auth

import es.joshluq.authkit.sdk.AuthKit
import es.joshluq.authkit.session.model.SessionState
import es.joshluq.authkit.session.model.TokenHolder
import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.AuthSessionState
import es.joshluq.kmsafe.infrastructure.mapper.toDomain
import es.joshluq.kmsafe.infrastructure.remote.model.UserSessionModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface to abstract session data access within the data layer.
 */
interface UserSessionDataSource {
    /** Returns the current session data or null if not available. */
    suspend fun getCurrentUserSession(): UserSessionModel?

    /** Returns a flow of the current session data. */
    fun observeUserSession(): Flow<UserSessionModel?>

    /** Returns a flow of the current session state as a domain-owned [AuthSessionState]. */
    fun getSessionState(): Flow<AuthSessionState>

    /** Returns whether a specific feature is enabled in the current session. */
    suspend fun hasFeature(featureId: String): Boolean

    /** Starts a new session with the provided tokens. */
    suspend fun startSession(tokens: TokenHolder)

    /** Saves user data for the current session. */
    suspend fun saveSessionData(data: UserSessionModel)

    /** Ends the current session. */
    suspend fun endSession()
}

/**
 * Implementation of [UserSessionDataSource] using [AuthKit].
 */
@Singleton
class UserSessionDataSourceImpl @Inject constructor(
    private val authKit: AuthKit,
    private val logger: LoggerKit
) : UserSessionDataSource {

    private val _sessionDataUpdates = MutableSharedFlow<UserSessionModel?>(replay = 1)

    override suspend fun getCurrentUserSession(): UserSessionModel? {
        val session = authKit.session.getSessionData<UserSessionModel>()
        logger.d("UserSessionDataSource", "getCurrentUserSession called -> $session")
        return session
    }

    override fun observeUserSession(): Flow<UserSessionModel?> {
        return combine(
            authKit.session.state,
            _sessionDataUpdates.onStart { emit(null) }
        ) { state, localUpdate ->
            logger.d("UserSessionDataSource", "observeUserSession triggered: authKitState=$state, localUpdate=$localUpdate")
            val sessionModel = if (state == SessionState.Active || state == SessionState.ExpiringSoon) {
                val fetched = localUpdate ?: authKit.session.getSessionData<UserSessionModel>()
                logger.d("UserSessionDataSource", "observeUserSession: fetched sessionModel=$fetched")
                fetched
            } else {
                logger.d("UserSessionDataSource", "observeUserSession: state is not active ($state), returning null")
                null
            }
            sessionModel
        }.distinctUntilChanged().onEach { model ->
            logger.d("UserSessionDataSource", "observeUserSession emitted downstream: $model")
        }
    }

    override fun getSessionState(): Flow<AuthSessionState> {
        return authKit.session.state.map {
            val domainState = it.toDomain()
            logger.d("UserSessionDataSource", "getSessionState: authKitState=$it -> domainState=$domainState")
            domainState
        }
    }

    override suspend fun hasFeature(featureId: String): Boolean {
        val session = getCurrentUserSession() ?: return false
        val entitlements = session.entitlements ?: return false
        return entitlements.subscriptionLevel == "PREMIUM" || entitlements.enabledFeatures.contains(featureId)
    }

    override suspend fun startSession(tokens: TokenHolder) {
        logger.i("UserSessionDataSource", "Starting session with tokens")
        authKit.session.startSession(tokens)
    }

    override suspend fun saveSessionData(data: UserSessionModel) {
        logger.i("UserSessionDataSource", "Saving session data: $data")
        authKit.session.saveSessionData(data)
        _sessionDataUpdates.emit(data)
    }

    override suspend fun endSession() {
        logger.i("UserSessionDataSource", "Ending session")
        authKit.session.endSession()
        _sessionDataUpdates.emit(null)
    }
}
