package es.joshluq.kmsafe.infrastructure.repository

import es.joshluq.foundationkit.log.LoggerKit
import es.joshluq.kmsafe.domain.model.AuthSessionState
import es.joshluq.kmsafe.domain.model.KmException
import es.joshluq.kmsafe.infrastructure.mapper.ErrorMapper
import es.joshluq.kmsafe.infrastructure.mapper.toDomain
import es.joshluq.kmsafe.infrastructure.remote.api.EntitlementsApiService
import es.joshluq.kmsafe.infrastructure.remote.api.StartTrialRequest
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.repository.AuthRepository
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [EntitlementsRepository] managing user feature permissions and subscriptions.
 *
 * Implements a time-to-live (TTL) memory cache for remote entitlement queries and reacts to session
 * state transitions to invalidate cached permissions when the user signs out.
 *
 * @property apiService Remote API service for querying entitlements and activating trials.
 * @property authRepository Repository used to store and synchronize entitlements with the active session.
 */
@Singleton
class EntitlementsRepositoryImpl @Inject constructor(
    private val apiService: EntitlementsApiService,
    private val authRepository: AuthRepository,
    private val errorMapper: ErrorMapper,
    private val logger: LoggerKit
) : EntitlementsRepository {

    companion object {
        private const val CACHE_TTL_MILLIS = 15 * 60 * 1000L // 15 minutes
    }

    private var lastFetchTime = 0L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        observeSession()
    }

    private fun observeSession() {
        authRepository.getSessionState()
            .onEach { state ->
                if (state is AuthSessionState.Idle) {
                    clearCache()
                }
            }
            .launchIn(scope)
    }

    /**
     * Retrieves the current user entitlements, checking the remote service if the cache expired or if [forceRefresh] is true.
     *
     * @param deviceFingerprint Unique identifier representing the hardware/install instance for trial eligibility checks.
     * @param forceRefresh When true, bypasses the TTL cache and queries the backend directly.
     * @return Flow emitting the resolved [Entitlements] from the persistent session.
     */
    override fun getEntitlements(deviceFingerprint: String, forceRefresh: Boolean): Flow<Entitlements> = flow {
        val currentTime = System.currentTimeMillis()
        if (forceRefresh || currentTime - lastFetchTime > CACHE_TTL_MILLIS) {
            val responseResult = runCatching {
                apiService.getEntitlements(deviceFingerprint)
            }
            val response = responseResult.getOrNull()
            if (response != null && response.isSuccessful) {
                val domainEntitlements = response.body()?.toDomain() ?: Entitlements.Default
                logger.i("EntitlementsRepository", "Remote entitlements fetched successfully: $domainEntitlements")
                authRepository.updateEntitlements(domainEntitlements)
                lastFetchTime = currentTime
                emit(domainEntitlements)
                return@flow
            } else {
                logger.e(
                    "EntitlementsRepository",
                    "Failed to fetch remote entitlements: isSuccessful=${response?.isSuccessful}, code=${response?.code()}, exception=${responseResult.exceptionOrNull()?.message}"
                )
            }
        }
        emitAll(authRepository.getEntitlements())
    }

    /**
     * Activates a promotional or trial period for the given device fingerprint.
     *
     * @param deviceFingerprint Unique identifier of the device requesting the trial.
     * @return Flow emitting the updated [Entitlements] reflecting the trial status.
     */
    override fun startTrial(deviceFingerprint: String): Flow<Entitlements> = flow {
        val response = apiService.startTrial(StartTrialRequest(deviceFingerprint))
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.success) {
                val domainEntitlements = body.entitlements?.toDomain() ?: Entitlements.Default
                authRepository.updateEntitlements(domainEntitlements)
                lastFetchTime = System.currentTimeMillis()
            } else {
                throw KmException(errorMapper.mapApiResponse(response, body?.error))
            }
        } else {
            throw KmException(errorMapper.mapApiResponse(response))
        }
        emitAll(authRepository.getEntitlements())
    }

    /**
     * Continuously observes the entitlement stream from the authenticated session.
     */
    override fun observeEntitlements(): Flow<Entitlements> = authRepository.getEntitlements()

    /**
     * Resets the TTL cache timestamp to force a remote fetch on next read.
     */
    override fun clearCache() {
        lastFetchTime = 0L
    }
}
