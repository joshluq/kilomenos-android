package es.joshluq.kmsafe.data.remote.model

import kotlinx.serialization.Serializable

/**
 * Serializable data model for storing user entitlements in the session.
 */
@Serializable
data class EntitlementsModel(
    val subscriptionLevel: String,
    val isTrialActive: Boolean,
    val trialExpiresAt: Long?,
    val enabledFeatures: List<String>,
    val canStartTrial: Boolean,
    val trialFeatures: List<String>
)
