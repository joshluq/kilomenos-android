package es.joshluq.kmsafe.domain.model

import es.joshluq.foundationkit.text.TextProvider

/**
 * Universal state representing full-screen, blocking HUD overlays across the application.
 *
 * Designed for single-window architecture to avoid secondary Dialog window artifacts
 * (such as system bar color flicker and edge-to-edge resets) and strictly block user interaction
 * across all navigation destinations while long-running or destructive operations are in flight.
 */
sealed interface AppOverlayState {

    /**
     * No blocking HUD overlay is displayed; standard user interaction enabled.
     */
    data object None : AppOverlayState

    /**
     * Displayed when the active renting contract / vehicle is being switched and synced.
     *
     * @param vehicleName Optional display name of the target vehicle.
     */
    data class VehicleSwitching(
        val vehicleName: String?
    ) : AppOverlayState

    /**
     * Displayed during session termination and secure token revocation.
     */
    data object LoggingOut : AppOverlayState

    /**
     * Displayed during GDPR account deletion and local database purging.
     *
     * @param stepMessage Current explanatory stage of the deletion process.
     * @param progress Optional fractional progress indicator (0.0 to 1.0).
     */
    data class AccountDeletion(
        val stepMessage: TextProvider? = null,
        val progress: Float? = null
    ) : AppOverlayState

    /**
     * Displayed during OCR optical receipt scanning and AI entity extraction.
     *
     * @param stepMessage Optional contextual status message.
     */
    data class AiReceiptScanning(
        val stepMessage: TextProvider? = null
    ) : AppOverlayState
}
