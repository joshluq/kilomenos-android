package es.joshluq.kmsafe.data.repository

import es.joshluq.authkit.session.model.SessionState
import es.joshluq.kmsafe.data.mapper.toDomain
import es.joshluq.kmsafe.data.remote.api.EntitlementsApiService
import es.joshluq.kmsafe.data.remote.api.StartTrialRequest
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.repository.AuthRepository
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntitlementsRepositoryImpl @Inject constructor(
    private val apiService: EntitlementsApiService,
    private val authRepository: AuthRepository
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
                if (state is SessionState.Idle) {
                    clearCache()
                }
            }
            .launchIn(scope)
    }

    override fun getEntitlements(deviceFingerprint: String, forceRefresh: Boolean): Flow<Entitlements> = flow {
        val currentTime = System.currentTimeMillis()
        if (forceRefresh || currentTime - lastFetchTime > CACHE_TTL_MILLIS) {
            runCatching {
                val response = apiService.getEntitlements(deviceFingerprint)
                if (response.isSuccessful) {
                    val domainEntitlements = response.body()?.toDomain() ?: Entitlements.Default
                    authRepository.updateEntitlements(domainEntitlements)
                    lastFetchTime = currentTime
                }
            }
        }
        emit(authRepository.getEntitlements().first())
    }

    override fun startTrial(deviceFingerprint: String): Flow<Entitlements> = flow {
        runCatching {
            val response = apiService.startTrial(StartTrialRequest(deviceFingerprint))
            if (response.isSuccessful) {
                val domainEntitlements = response.body()?.entitlements?.toDomain() ?: Entitlements.Default
                authRepository.updateEntitlements(domainEntitlements)
                lastFetchTime = System.currentTimeMillis()
            }
        }
        emit(authRepository.getEntitlements().first())
    }

    override fun observeEntitlements(): Flow<Entitlements> = authRepository.getEntitlements()

    override fun clearCache() {
        lastFetchTime = 0L
    }
}
