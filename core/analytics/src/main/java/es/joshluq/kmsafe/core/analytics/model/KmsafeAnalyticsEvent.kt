package es.joshluq.kmsafe.core.analytics.model

/**
 * Root sealed hierarchy for all strongly-typed analytics events across KiloMenos.
 */
sealed interface KmsafeAnalyticsEvent {
    val name: String
    val properties: Map<String, Any> get() = emptyMap()

    // 1. Auth & Identity Funnel
    sealed interface Auth : KmsafeAnalyticsEvent {
        data class SignUpStarted(val method: String = "credentials") : Auth {
            override val name: String = "signup_started"
            override val properties: Map<String, Any> = mapOf("method" to method)
        }
        data class SignUpCompleted(val method: String = "credentials") : Auth {
            override val name: String = "signup_completed"
            override val properties: Map<String, Any> = mapOf("method" to method)
        }
        data class SignUpFailed(val error: String) : Auth {
            override val name: String = "signup_failed"
            override val properties: Map<String, Any> = mapOf("error" to error)
        }
        data class LoginStarted(val method: String = "credentials") : Auth {
            override val name: String = "login_started"
            override val properties: Map<String, Any> = mapOf("method" to method)
        }
        data class LoginCompleted(val method: String = "credentials") : Auth {
            override val name: String = "login_completed"
            override val properties: Map<String, Any> = mapOf("method" to method)
        }
        data class LoginFailed(val error: String, val method: String = "credentials") : Auth {
            override val name: String = "login_failed"
            override val properties: Map<String, Any> = mapOf("error" to error, "method" to method)
        }
        data object UserConflictAlertShown : Auth {
            override val name: String = "user_conflict_alert_shown"
        }
    }

    // 2. Onboarding & Fleet Setup Funnel
    sealed interface Onboarding : KmsafeAnalyticsEvent {
        data class WizardStarted(val isFirstVehicle: Boolean, val source: String = "onboarding") : Onboarding {
            override val name: String = "onboarding_wizard_started"
            override val properties: Map<String, Any> = mapOf("is_first_vehicle" to isFirstVehicle, "source" to source)
        }
        data class StepCompleted(val stepIndex: Int, val stepName: String, val extra: Map<String, Any> = emptyMap()) : Onboarding {
            override val name: String = "onboarding_step_completed"
            override val properties: Map<String, Any> = mapOf("step_index" to stepIndex, "step_name" to stepName) + extra
        }
        data class StepAbandoned(val stepName: String, val timeSpentSeconds: Long = 0) : Onboarding {
            override val name: String = "onboarding_step_abandoned"
            override val properties: Map<String, Any> = mapOf("step_name" to stepName, "time_spent_seconds" to timeSpentSeconds)
        }
        data class RentingSetupCompleted(val hasBluetooth: Boolean, val hasAdvancedPricing: Boolean) : Onboarding {
            override val name: String = "renting_setup_completed"
            override val properties: Map<String, Any> = mapOf("has_bluetooth" to hasBluetooth, "has_advanced" to hasAdvancedPricing)
        }
        data object MultiVehicleLimitReached : Onboarding {
            override val name: String = "multi_vehicle_limit_reached"
        }
    }

    // 3. Overview & Habit Loop
    sealed interface Overview : KmsafeAnalyticsEvent {
        data class OdometerUpdated(val value: Double, val deltaKm: Double? = null) : Overview {
            override val name: String = "odometer_updated"
            override val properties: Map<String, Any> = mutableMapOf<String, Any>("value" to value).apply {
                deltaKm?.let { put("delta_km", it) }
            }
        }
        data class FuelEntryAdded(val amount: Double) : Overview {
            override val name: String = "fuel_entry_added"
            override val properties: Map<String, Any> = mapOf("amount" to amount)
        }
        data class StatusCapsuleClicked(val type: String) : Overview {
            override val name: String = "status_capsule_clicked"
            override val properties: Map<String, Any> = mapOf("type" to type)
        }
        data object ProjectionBannerClicked : Overview {
            override val name: String = "projection_banner_clicked"
        }
        data object AutoTrackingPromotionAccepted : Overview {
            override val name: String = "autotracking_promotion_accepted"
        }
        data object AutoTrackingPromotionDismissed : Overview {
            override val name: String = "autotracking_promotion_dismissed"
        }
    }

    // 4. Tracking & Telemetry
    sealed interface Tracking : KmsafeAnalyticsEvent {
        data object TrackingStarted : Tracking {
            override val name: String = "tracking_started"
        }
        data class TrackingStopped(val distanceMeters: Double) : Tracking {
            override val name: String = "tracking_stopped"
            override val properties: Map<String, Any> = mapOf("distance" to distanceMeters)
        }
        data object TrackingCancelled : Tracking {
            override val name: String = "tracking_cancelled"
        }
        data class TripAutoDetected(val trigger: String) : Tracking {
            override val name: String = "autotracking_trip_detected"
            override val properties: Map<String, Any> = mapOf("trigger" to trigger)
        }
        data class TripConfirmed(val distanceKm: Double, val accuracyScore: Float? = null) : Tracking {
            override val name: String = "autotracking_trip_confirmed"
            override val properties: Map<String, Any> = mutableMapOf<String, Any>("distance_km" to distanceKm).apply {
                accuracyScore?.let { put("accuracy_score", it) }
            }
        }
        data object BtValidationSkippedNoPermission : Tracking {
            override val name: String = "tracking_bt_validation_skipped_no_permission"
        }
        data object GpsSpoofingDetected : Tracking {
            override val name: String = "security_gps_spoofing_detected"
        }
    }

    // 5. Monetization & Paywall Funnel
    sealed interface Monetization : KmsafeAnalyticsEvent {
        data class PaywallViewed(val source: String) : Monetization {
            override val name: String = "premium_paywall_viewed"
            override val properties: Map<String, Any> = mapOf("source" to source)
        }
        data class PlanSelected(val plan: String) : Monetization {
            override val name: String = "premium_plan_selected"
            override val properties: Map<String, Any> = mapOf("plan" to plan)
        }
        data class UpgradeClicked(val source: String, val selectedPlan: String? = null) : Monetization {
            override val name: String = "premium_upgrade_clicked"
            override val properties: Map<String, Any> = mutableMapOf<String, Any>("source" to source).apply {
                selectedPlan?.let { put("selected_plan", it) }
            }
        }
        data class PurchaseResult(val result: String, val errorCode: String? = null, val plan: String? = null) : Monetization {
            override val name: String = "billing_result_received"
            override val properties: Map<String, Any> = mutableMapOf<String, Any>("result" to result).apply {
                errorCode?.let { put("error_code", it) }
                plan?.let { put("plan", it) }
            }
        }
        data class UpgradeSuccess(val plan: String? = null) : Monetization {
            override val name: String = "premium_upgrade_success"
            override val properties: Map<String, Any> = mutableMapOf<String, Any>().apply {
                plan?.let { put("plan", it) }
            }
        }
        data object PaywallDismissed : Monetization {
            override val name: String = "premium_paywall_dismissed"
        }
        data class TrialStarted(val source: String = "preferences") : Monetization {
            override val name: String = "premium_trial_started"
            override val properties: Map<String, Any> = mapOf("source" to source)
        }
        data object TrialOfferDismissed : Monetization {
            override val name: String = "premium_trial_offer_dismissed"
        }
    }

    // 6. Expenses & Stations
    sealed interface Expenses : KmsafeAnalyticsEvent {
        data class ScreenViewed(val fuelType: String) : Expenses {
            override val name: String = "expenses_screen_viewed"
            override val properties: Map<String, Any> = mapOf("fuel_type" to fuelType)
        }
        data class ExpenseSaved(val energyType: String, val amountEur: Double, val unitPrice: Double? = null) : Expenses {
            override val name: String = "expense_saved"
            override val properties: Map<String, Any> = mutableMapOf<String, Any>(
                "energy_type" to energyType,
                "amount_eur" to amountEur
            ).apply {
                unitPrice?.let { put("unit_price", it) }
            }
        }
        data class StationVolatilityViewed(val stationBrand: String, val variancePct: Double) : Expenses {
            override val name: String = "station_volatility_viewed"
            override val properties: Map<String, Any> = mapOf("brand" to stationBrand, "variance_pct" to variancePct)
        }
        data class ElectrificationKpiInspected(val savingsEur: Double, val kwhConsumed: Double) : Expenses {
            override val name: String = "electrification_kpi_inspected"
            override val properties: Map<String, Any> = mapOf("savings_eur" to savingsEur, "kwh" to kwhConsumed)
        }
    }

    // 7. Projection & Risk Sentinel
    sealed interface Projection : KmsafeAnalyticsEvent {
        data object ProjectionViewed : Projection {
            override val name: String = "projection_viewed"
        }
        data class FinancialImpactViewed(
            val isOverLimit: Boolean,
            val estimatedPenalty: Double,
            val billableExcessKms: Double
        ) : Projection {
            override val name: String = "projection_financial_impact_viewed"
            override val properties: Map<String, Any> = mapOf(
                "is_over_limit" to isOverLimit,
                "estimated_penalty" to estimatedPenalty,
                "billable_excess_kms" to billableExcessKms
            )
        }
        data class ConfigureContractClicked(val vehicleId: String, val source: String) : Projection {
            override val name: String = "projection_configure_contract_clicked"
            override val properties: Map<String, Any> = mapOf("vehicle_id" to vehicleId, "source" to source)
        }
        data object MultiTripBlockedFree : Projection {
            override val name: String = "projection_multi_trip_blocked_free"
        }
    }

    // 8. History & Audit
    sealed interface History : KmsafeAnalyticsEvent {
        data class RouteTeaserViewed(val recordId: String) : History {
            override val name: String = "route_teaser_viewed"
            override val properties: Map<String, Any> = mapOf("record_id" to recordId)
        }
        data class RouteMapViewed(val recordId: String, val points: Int) : History {
            override val name: String = "route_map_viewed"
            override val properties: Map<String, Any> = mapOf("record_id" to recordId, "points" to points)
        }
        data class FuelEntryAdded(val amount: Double) : History {
            override val name: String = "fuel_entry_added"
            override val properties: Map<String, Any> = mapOf("amount" to amount)
        }
        data class CertifiedExportClicked(val format: String = "CSV") : History {
            override val name: String = "certified_audit_exported"
            override val properties: Map<String, Any> = mapOf("format" to format)
        }
        data class HistoryViewed(val totalRecords: Int, val totalKms: Double) : History {
            override val name: String = "history_viewed"
            override val properties: Map<String, Any> = mapOf("total_records" to totalRecords, "total_kms" to totalKms)
        }
        data class GroupingModeChanged(val mode: String) : History {
            override val name: String = "history_grouping_mode_changed"
            override val properties: Map<String, Any> = mapOf("mode" to mode)
        }
        data class SearchPerformed(val queryLength: Int) : History {
            override val name: String = "history_search_performed"
            override val properties: Map<String, Any> = mapOf("query_length" to queryLength)
        }
        data class RecordDeleted(val recordId: String) : History {
            override val name: String = "record_deleted"
            override val properties: Map<String, Any> = mapOf("record_id" to recordId)
        }
    }

    // 9. Profile & Churn
    sealed interface Profile : KmsafeAnalyticsEvent {
        data object LogoutConfirmed : Profile {
            override val name: String = "logout"
        }
        data object AccountDeletionConfirmed : Profile {
            override val name: String = "account_deleted"
        }
        data class PreferenceToggled(val key: String, val enabled: Boolean) : Profile {
            override val name: String = "${key}_toggled"
            override val properties: Map<String, Any> = mapOf("enabled" to enabled)
        }
        data object ManagePrivacyClicked : Profile {
            override val name: String = "manage_privacy_clicked"
        }
    }

    // 10. Fleet
    sealed interface Fleet : KmsafeAnalyticsEvent {
        data object VehicleSwitched : Fleet {
            override val name: String = "vehicle_switched"
        }
        data class VehicleDeleted(val vehicleId: String? = null) : Fleet {
            override val name: String = "vehicle_deleted"
            override val properties: Map<String, Any> = mutableMapOf<String, Any>().apply {
                vehicleId?.let { put("id", it) }
            }
        }
        data class VehicleUpdated(val vehicleId: String) : Fleet {
            override val name: String = "vehicle_updated"
            override val properties: Map<String, Any> = mapOf("id" to vehicleId)
        }
    }

    // Generic fallback for ad-hoc custom events if needed
    data class Custom(
        override val name: String,
        override val properties: Map<String, Any> = emptyMap()
    ) : KmsafeAnalyticsEvent
}
