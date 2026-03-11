package es.joshluq.kmsafe.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Interface for all navigation destinations in the application.
 */
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
     * RentingDetails destination (Onboarding screen).
     */
    @Serializable
    data class RentingDetails(
        val vehicleId: String? = null,
        val isEdit: Boolean = false
    ) : Destination

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
}
