package es.joshluq.kmsafe.data.repository

import es.joshluq.kmsafe.data.mapper.toDomain
import es.joshluq.kmsafe.data.remote.api.EntitlementsApiService
import es.joshluq.kmsafe.data.remote.api.StartTrialRequest
import es.joshluq.kmsafe.domain.model.Entitlements
import es.joshluq.kmsafe.domain.repository.EntitlementsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntitlementsRepositoryImpl @Inject constructor(
    private val apiService: EntitlementsApiService
) : EntitlementsRepository {

    companion object {
        private const val CACHE_TTL_MILLIS = 15 * 60 * 1000L // 15 minutes

    }
    private val _entitlements = MutableStateFlow(Entitlements.Default)
    private var lastFetchTime = 0L

    override fun getEntitlements(deviceFingerprint: String): Flow<Entitlements> = flow {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFetchTime > CACHE_TTL_MILLIS) {
            runCatching {
                val response = apiService.getEntitlements(deviceFingerprint)
                if (response.isSuccessful) {
                    val domainEntitlements = response.body()?.toDomain() ?: Entitlements.Default
                    _entitlements.value = domainEntitlements
                    lastFetchTime = currentTime
                }
            }
        }
        emit(_entitlements.value)
    }

    override fun startTrial(deviceFingerprint: String): Flow<Entitlements> = flow {
        runCatching {
            val response = apiService.startTrial(StartTrialRequest(deviceFingerprint))
            if (response.isSuccessful) {
                val domainEntitlements = response.body()?.entitlements?.toDomain() ?: Entitlements.Default
                _entitlements.value = domainEntitlements
                lastFetchTime = System.currentTimeMillis()
            }
        }
        emit(_entitlements.value)
    }

    override fun observeEntitlements(): Flow<Entitlements> = _entitlements.asStateFlow()
}
