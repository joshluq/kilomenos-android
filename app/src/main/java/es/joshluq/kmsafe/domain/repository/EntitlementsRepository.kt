package es.joshluq.kmsafe.domain.repository

import es.joshluq.kmsafe.domain.model.Entitlements
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user entitlements and trial access.
 */
interface EntitlementsRepository {
    /**
     * Retrieves the current user entitlements from the local cache
     * or remote server if the cache is expired.
     */
    fun getEntitlements(deviceFingerprint: String): Flow<Entitlements>

    /**
     * Initiates the 7-day trial period for the current user and device.
     */
    fun startTrial(deviceFingerprint: String): Flow<Entitlements>
    
    /**
     * Checks if a specific feature is enabled in the current entitlements.
     */
    fun observeEntitlements(): Flow<Entitlements>
}
