package es.joshluq.kmsafe.infrastructure.remote.response

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Data Transfer Object for user entitlements and trial status.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class EntitlementsResponse {
    @JsonProperty("subscription_level")
    val subscriptionLevel: String? = null

    @JsonProperty("trial_active")
    val trialActive: Boolean? = null

    @JsonProperty("trial_expires_at")
    val trialExpiresAt: String? = null

    @JsonProperty("enabled_features")
    val enabledFeatures: List<String>? = null

    @JsonProperty("can_start_trial")
    val canStartTrial: Boolean? = null

    @JsonProperty("trial_features")
    val trialFeatures: List<String>? = null
}

/**
 * Wrapper response for the start trial endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class StartTrialResponse : NetworkResponse() {
    @JsonProperty("entitlements")
    val entitlements: EntitlementsResponse? = null
}
