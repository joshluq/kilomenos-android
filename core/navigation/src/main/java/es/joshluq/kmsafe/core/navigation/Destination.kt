package es.joshluq.kmsafe.core.navigation

import kotlinx.serialization.Serializable

/**
 * Deep link configuration constants.
 */
object DeepLinkConfig {
    const val SCHEME = "https"
    const val HOST = "kmsafe.app"
    const val BASE_URL = "$SCHEME://$HOST"
}

/**
 * Interface for all navigation destinations in the application.
 */
@Serializable
sealed interface Destination {

    /**
     * Launch (Splash) destination.
     */
    @Serializable
    object Launch : Destination

    /**
     * Dashboard container destination.
     */
    @Serializable
    object Dashboard : Destination

    /**
     * Overview destination.
     * Hosted inside Dashboard.
     */
    @Serializable
    object Overview : Destination

    /**
     * Login destination.
     */
    @Serializable
    object Login : Destination

    /**
     * Signup destination.
     */
    @Serializable
    object Signup : Destination

    /**
     * Profile destination.
     */
    @Serializable
    object Profile : Destination

    /**
     * Premium Paywall destination.
     */
    @Serializable
    object PremiumPaywall : Destination

    /**
     * Welcome Discovery destination.
     * @param isGuideMode True when opened as an informative guide from profile/settings.
     */
    @Serializable
    data class WelcomeDiscovery(val isGuideMode: Boolean = false) : Destination

    /**
     * EditContract destination.
     */
    @Serializable
    data class EditContract(val vehicleId: String) : Destination

    /**
     * SetupWizard destination (New creation flow).
     */
    @Serializable
    object SetupWizard : Destination

    /**
     * VehicleDetail destination (Dynamic visualization).
     */
    @Serializable
    data class VehicleDetail(val vehicleId: String) : Destination

    /**
     * History destination.
     */
    @Serializable
    object History : Destination

    /**
     * ProjectionAnalysis destination.
     */
    @Serializable
    object ProjectionAnalysis : Destination

    /**
     * DataManagement destination.
     */
    @Serializable
    object DataManagement : Destination

    /**
     * VehicleList destination.
     */
    @Serializable
    object VehicleList : Destination

    /**
     * Preferences destination.
     */
    @Serializable
    object Preferences : Destination

    /**
     * Odometer Record Detail destination.
     */
    @Serializable
    data class RecordDetail(val recordId: String) : Destination

    /**
     * Image Cropper destination.
     */
    @Serializable
    data class ImageCropper(val uri: String) : Destination

    /**
     * Auto Tracking Permissions Rationale destination.
     */
    @Serializable
    object AutoTrackingPermissions : Destination

    /**
     * Assisted Tracking Permissions Rationale destination (Free tier manual tracking).
     */
    @Serializable
    object AssistedTrackingPermissions : Destination

    /**
     * Expenses destination.
     */
    @Serializable
    data class Expenses(
        val stationId: String? = null,
        val autoOpenAdd: Boolean = false,
        val priceReportMode: Boolean = false
    ) : Destination

    /**
     * Station Management destination.
     */
    @Serializable
    object StationManagement : Destination

    /**
     * Station Detail destination.
     */
    @Serializable
    data class StationDetail(val stationId: String) : Destination
}
